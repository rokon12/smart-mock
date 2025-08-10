package ca.bazlur.smartmock.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OpenApiIndex {

    // pattern -> (METHOD -> Endpoint)
    private final Map<String, Map<String, Endpoint>> endpoints = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Getter
    private volatile OpenAPI openAPI;

    @Getter
    private volatile String rawSpecContent;

    public void loadSpec(String specContent) {
        this.rawSpecContent = specContent;
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent);
        if (result == null || result.getOpenAPI() == null) {
            log.error("Failed to parse OpenAPI spec from raw content. Messages: {}", result != null ? result.getMessages() : "none");
            this.openAPI = null;
            endpoints.clear();
            return;
        }
        this.openAPI = result.getOpenAPI();
        indexEndpoints();
    }

    public void loadSpecFromFile(String filePath) {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(filePath, null, null);
        if (result == null || result.getOpenAPI() == null) {
            log.error("Failed to parse OpenAPI spec from file: {}. Messages: {}", filePath, result != null ? result.getMessages() : "none");
            this.openAPI = null;
            this.rawSpecContent = null;
            endpoints.clear();
            return;
        }
        this.openAPI = result.getOpenAPI();
        this.rawSpecContent = null; // unknown
        indexEndpoints();
    }
    
    public void clear() {
        this.openAPI = null;
        this.rawSpecContent = null;
        this.endpoints.clear();
        log.info("OpenAPI spec cleared");
    }

    private void indexEndpoints() {
        endpoints.clear();
        if (openAPI == null || openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
            log.warn("No paths to index.");
            return;
        }

        openAPI.getPaths().forEach((path, pathItem) -> {
            if (pathItem == null) return;
            Map<String, Endpoint> methodMap = new LinkedHashMap<>();

            putIfOp(methodMap, path, "GET", pathItem.getGet());
            putIfOp(methodMap, path, "POST", pathItem.getPost());
            putIfOp(methodMap, path, "PUT", pathItem.getPut());
            putIfOp(methodMap, path, "DELETE", pathItem.getDelete());
            putIfOp(methodMap, path, "PATCH", pathItem.getPatch());
            putIfOp(methodMap, path, "HEAD", pathItem.getHead());
            putIfOp(methodMap, path, "OPTIONS", pathItem.getOptions());
            putIfOp(methodMap, path, "TRACE", pathItem.getTrace());

            if (!methodMap.isEmpty()) {
                endpoints.put(convertToAntPattern(path), methodMap);
            }
        });

        log.info("Indexed {} path patterns from OpenAPI spec", endpoints.size());
    }

    private void putIfOp(Map<String, Endpoint> map, String path, String method, Operation op) {
        if (op == null) return;
        List<Parameter> params = op.getParameters() != null ? op.getParameters() : Collections.emptyList();
        map.put(method, Endpoint.builder()
            .path(path)
            .method(method)
            .operation(op)
            .parameters(params)
            .responses(op.getResponses())
            .build());
    }

    // `{id}` -> `*` (single segment). Keep the number of segments identical.
    private String convertToAntPattern(String openApiPath) {
        if (openApiPath == null || openApiPath.isBlank()) return openApiPath;
        return openApiPath.replaceAll("\\{[^/}]+}", "*");
    }

    public Optional<Endpoint> match(String method, String requestPath) {
        if (method == null || requestPath == null) return Optional.empty();
        String normalizedMethod = method.toUpperCase(Locale.ROOT);

        // Gather all matching patterns for this path
        List<Map.Entry<String, Map<String, Endpoint>>> candidates = new ArrayList<>();
        for (var e : endpoints.entrySet()) {
            if (pathMatcher.match(e.getKey(), requestPath)) {
                candidates.add(e);
            }
        }
        if (candidates.isEmpty()) return Optional.empty();

        // Choose the most specific pattern
        candidates.sort(Comparator.comparingInt((Map.Entry<String, Map<String, Endpoint>> e) -> specificityScore(e.getKey()))
            .reversed());

        for (var e : candidates) {
            Endpoint ep = e.getValue().get(normalizedMethod);
            if (ep != null) return Optional.of(ep);
        }
        return Optional.empty();
    }

    // Higher score = more specific (longer, fewer wildcards)
    private int specificityScore(String pattern) {
        int length = pattern.length();
        int wildcards = (int) pattern.chars().filter(ch -> ch == '*' || ch == '?').count();
        return (length * 10) - (wildcards * 100); // weight wildcards heavily
    }

    public Schema<?> getSchemaFromResponse(ApiResponse response) {
        if (response == null) return null;
        Content content = response.getContent();
        if (content == null || content.isEmpty()) return null;

        // Prefer application/json
        MediaType mt = content.get("application/json");
        if (mt != null) return mt.getSchema();

        // Then any +json (e.g., application/merge-patch+json)
        for (Map.Entry<String, MediaType> e : content.entrySet()) {
            String key = e.getKey() != null ? e.getKey().toLowerCase(Locale.ROOT) : "";
            if (key.endsWith("+json")) return e.getValue().getSchema();
        }

        // Fallback to first
        return content.values().iterator().next().getSchema();
    }

    public Schema<?> resolveSchema(Schema<?> schema) {
        return resolveSchema(schema, new HashSet<>());
    }

    private Schema<?> resolveSchema(Schema<?> schema, Set<String> seenRefs) {
        if (schema == null) return null;

        String ref = schema.get$ref();
        if (ref == null || ref.isBlank()) return schema;

        String name = ref.substring(ref.lastIndexOf('/') + 1);
        if (openAPI == null || openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return schema; // cannot resolve
        }
        if (!seenRefs.add(name)) {
            log.warn("Cyclic $ref detected for schema: {}", name);
            return schema;
        }

        Schema<?> target = openAPI.getComponents().getSchemas().get(name);
        if (target == null) {
            log.warn("Missing component schema for $ref: {}", name);
            return schema;
        }
        // Resolve recursively in case target itself is a $ref
        return resolveSchema(target, seenRefs);
    }
}
