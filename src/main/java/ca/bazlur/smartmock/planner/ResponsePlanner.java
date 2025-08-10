package ca.bazlur.smartmock.planner;

import ca.bazlur.smartmock.model.Plan;
import ca.bazlur.smartmock.model.Scenario;
import ca.bazlur.smartmock.openapi.Endpoint;
import ca.bazlur.smartmock.openapi.OpenApiIndex;
import ca.bazlur.smartmock.util.JsonSchemaConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponsePlanner {
    private static final Pattern RANGE_2XX = Pattern.compile("^[2-9]XX$", Pattern.CASE_INSENSITIVE);

    private final OpenApiIndex openApiIndex;
    private final JsonSchemaConverter schemaConverter;
    private final ObjectMapper objectMapper;

    public Plan plan(Endpoint endpoint, Scenario scenario, HttpServletRequest request, String body) {
        int statusCode = determineStatusCode(endpoint, scenario, request);

        ApiResponse apiResponse = selectApiResponse(endpoint, statusCode);
        Schema<?> responseSchema = null;
        String jsonSchema = null;

        if (apiResponse != null) {
            String contentType = negotiateContentType(request, apiResponse.getContent());
            responseSchema = extractSchema(apiResponse.getContent(), contentType);
            if (responseSchema != null) {
                responseSchema = openApiIndex.resolveSchema(responseSchema);
                jsonSchema = schemaConverter.convertToJsonSchema(responseSchema);
            }
        }

        Map<String, Object> requestContext = buildRequestContext(endpoint, request, body);

        return Plan.builder()
            .scenario(scenario)
            .statusCode(statusCode)
            .responseSchema(responseSchema)
            .jsonSchema(jsonSchema)
            .requestContext(requestContext)
            .operationId(endpoint.getOperationId())
            .path(endpoint.getPath())
            .method(endpoint.getMethod())
            .build();
    }

    int determineStatusCode(Endpoint endpoint, Scenario scenario, HttpServletRequest request) {
        String statusOverride = request.getHeader("X-Mock-Status");
        if (statusOverride != null) {
            try {
                return Integer.parseInt(statusOverride.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid X-Mock-Status: {}", statusOverride);
            }
        }
        return switch (scenario) {
            case HAPPY, EDGE -> findSuccessStatus(endpoint);
            case INVALID -> 400;
            case RATE_LIMIT -> 429;
            case SERVER_ERROR -> 500;
        };
    }

    private ApiResponse selectApiResponse(Endpoint endpoint, int statusCode) {
        var responses = endpoint.getResponses();
        if (responses == null || responses.isEmpty()) return null;

        ApiResponse exact = responses.get(String.valueOf(statusCode));
        if (exact != null) return exact;

        // Handle range keys like "2XX"
        String familyKey = (statusCode / 100) + "XX";
        ApiResponse ranged = responses.get(familyKey);
        if (ranged != null) return ranged;

        ApiResponse def = responses.getDefault();
        if (def != null) return def;

        // As a last resort, try a reasonable 2xx response, then any
        return pickPreferredSuccess(responses).orElseGet(() -> responses.values().iterator().next());
    }

    private Optional<ApiResponse> pickPreferredSuccess(Map<String, ApiResponse> responses) {
        List<String> preferred = List.of("200", "201");
        for (String key : preferred) {
            if (responses.containsKey(key)) return Optional.of(responses.get(key));
        }
        // first explicit 2xx or "2XX"
        return responses.entrySet().stream()
            .filter(e -> is2xxKey(e.getKey()))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    private boolean is2xxKey(String key) {
        try {
            int code = Integer.parseInt(key);
            return code >= 200 && code < 300;
        } catch (NumberFormatException ignored) {
            return RANGE_2XX.matcher(key).matches();
        }
    }

    private int findSuccessStatus(Endpoint endpoint) {
        var responses = endpoint.getResponses();
        if (responses == null || responses.isEmpty()) return 200;

        // Prefer 200 → 201 → any 2xx → default → first
        if (responses.containsKey("200")) return 200;
        if (responses.containsKey("201")) return 201;

        return responses.keySet().stream()
            .filter(this::is2xxKey)
            .map(k -> k.equalsIgnoreCase("2XX") ? 200 : Integer.parseInt(k))
            .findFirst()
            .orElseGet(() -> responses.getDefault() != null ? 200 : 200);
    }

    private String negotiateContentType(HttpServletRequest request, Content content) {
        if (content == null || content.isEmpty()) return "application/json";

        String accept = Optional.ofNullable(request.getHeader("Accept")).orElse("*/*");
        // Very light negotiation: exact match → json → first
        if (content.containsKey(accept)) return accept;
        if (accept.contains("json")) {
            for (String key : content.keySet()) {
                if (key.toLowerCase(Locale.ROOT).contains("json")) return key;
            }
        }
        if (content.containsKey("application/json")) return "application/json";
        return content.keySet().iterator().next();
    }

    private Schema<?> extractSchema(Content content, String contentType) {
        if (content == null) return null;
        MediaType mt = content.get(contentType);
        if (mt == null && "application/json".equals(contentType)) {
            // some specs register as "application/*+json"
            for (Map.Entry<String, MediaType> e : content.entrySet()) {
                if (e.getKey().toLowerCase(Locale.ROOT).endsWith("+json")) {
                    mt = e.getValue();
                    break;
                }
            }
        }
        return mt != null ? mt.getSchema() : null;
    }

    private Map<String, Object> buildRequestContext(Endpoint endpoint, HttpServletRequest request, String body) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("method", request.getMethod());
        ctx.put("path", request.getRequestURI());
        putIfNonNull(ctx, "operationId", endpoint.getOperationId());
        putIfNonNull(ctx, "summary", endpoint.getSummary());

        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            ctx.put("queryString", request.getQueryString());
        }

        Map<String, Object> queryParams = new LinkedHashMap<>();
        request.getParameterMap().forEach((k, v) -> queryParams.put(k, v.length == 1 ? v[0] : Arrays.asList(v)));
        if (!queryParams.isEmpty()) ctx.put("query", Collections.unmodifiableMap(queryParams));

        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Enumeration<String> e = request.getHeaderNames(); e != null && e.hasMoreElements(); ) {
            String key = e.nextElement();
            if (!key.regionMatches(true, 0, "X-Mock-", 0, 7)) {
                headers.put(key.toLowerCase(Locale.ROOT), request.getHeader(key));
            }
        }
        if (!headers.isEmpty()) ctx.put("headers", Collections.unmodifiableMap(headers));

        if (body != null && !body.isBlank()) {
            Object parsed = body;
            try {
                parsed = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                // keep raw string if not JSON
            }
            ctx.put("requestBody", parsed);
        }

        if (endpoint.getParameters() != null && !endpoint.getParameters().isEmpty()) {
            ctx.put("parameters", endpoint.getParameters());
        }

        return Collections.unmodifiableMap(ctx);
    }

    private void putIfNonNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
