package ca.bazlur.smartmock.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
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
    private final Map<String, Map<String, Endpoint>> endpoints = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    @Getter
    private OpenAPI openAPI;

    public void loadSpec(String specContent) {
        this.openAPI = new OpenAPIV3Parser().readContents(specContent).getOpenAPI();
        indexEndpoints();
    }

    public void loadSpecFromFile(String filePath) {
        this.openAPI = new OpenAPIV3Parser().read(filePath);
        indexEndpoints();
    }

    private void indexEndpoints() {
        if (openAPI == null || openAPI.getPaths() == null) {
            return;
        }

        endpoints.clear();
        
        openAPI.getPaths().forEach((path, pathItem) -> {
            Map<String, Endpoint> methodMap = new HashMap<>();
            
            if (pathItem.getGet() != null) {
                methodMap.put("GET", createEndpoint(path, "GET", pathItem.getGet()));
            }
            if (pathItem.getPost() != null) {
                methodMap.put("POST", createEndpoint(path, "POST", pathItem.getPost()));
            }
            if (pathItem.getPut() != null) {
                methodMap.put("PUT", createEndpoint(path, "PUT", pathItem.getPut()));
            }
            if (pathItem.getDelete() != null) {
                methodMap.put("DELETE", createEndpoint(path, "DELETE", pathItem.getDelete()));
            }
            if (pathItem.getPatch() != null) {
                methodMap.put("PATCH", createEndpoint(path, "PATCH", pathItem.getPatch()));
            }
            
            endpoints.put(convertToAntPattern(path), methodMap);
        });
        
        log.info("Indexed {} endpoints from OpenAPI spec", endpoints.size());
    }

    private Endpoint createEndpoint(String path, String method, Operation operation) {
        return Endpoint.builder()
                .path(path)
                .method(method)
                .operation(operation)
                .parameters(operation.getParameters() != null ? operation.getParameters() : new ArrayList<>())
                .responses(operation.getResponses())
                .build();
    }

    private String convertToAntPattern(String openApiPath) {
        return openApiPath.replaceAll("\\{([^}]+)\\}", "*");
    }

    public Optional<Endpoint> match(String method, String path) {
        for (Map.Entry<String, Map<String, Endpoint>> entry : endpoints.entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                Map<String, Endpoint> methodMap = entry.getValue();
                if (methodMap.containsKey(method.toUpperCase())) {
                    return Optional.of(methodMap.get(method.toUpperCase()));
                }
            }
        }
        return Optional.empty();
    }

    public Schema<?> getSchemaFromResponse(ApiResponse response) {
        if (response.getContent() == null) {
            return null;
        }
        
        Content content = response.getContent();
        MediaType mediaType = content.get("application/json");
        if (mediaType == null) {
            mediaType = content.values().iterator().next();
        }
        
        return mediaType != null ? mediaType.getSchema() : null;
    }

    public Schema<?> resolveSchema(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
            if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
                return openAPI.getComponents().getSchemas().get(schemaName);
            }
        }
        
        return schema;
    }
}