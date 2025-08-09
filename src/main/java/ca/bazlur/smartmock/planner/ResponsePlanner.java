package ca.bazlur.smartmock.planner;

import ca.bazlur.smartmock.model.Plan;
import ca.bazlur.smartmock.model.Scenario;
import ca.bazlur.smartmock.openapi.Endpoint;
import ca.bazlur.smartmock.openapi.OpenApiIndex;
import ca.bazlur.smartmock.util.JsonSchemaConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponsePlanner {
    private final OpenApiIndex openApiIndex;
    private final JsonSchemaConverter schemaConverter;
    private final ObjectMapper objectMapper;

    public Plan plan(Endpoint endpoint, Scenario scenario, HttpServletRequest request, String body) {
        int statusCode = determineStatusCode(endpoint, scenario, request);
        ApiResponse apiResponse = endpoint.getResponses().get(String.valueOf(statusCode));
        
        if (apiResponse == null) {
            apiResponse = endpoint.getResponses().getDefault();
            if (apiResponse == null && !endpoint.getResponses().isEmpty()) {
                apiResponse = endpoint.getResponses().values().iterator().next();
            }
        }

        Schema<?> responseSchema = null;
        String jsonSchema = null;
        
        if (apiResponse != null) {
            responseSchema = openApiIndex.getSchemaFromResponse(apiResponse);
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

    private int determineStatusCode(Endpoint endpoint, Scenario scenario, HttpServletRequest request) {
        String statusOverride = request.getHeader("X-Mock-Status");
        if (statusOverride != null) {
            try {
                return Integer.parseInt(statusOverride);
            } catch (NumberFormatException e) {
                log.warn("Invalid status override: {}", statusOverride);
            }
        }

        return switch (scenario) {
            case HAPPY -> findSuccessStatus(endpoint);
            case INVALID -> 400;
            case RATE_LIMIT -> 429;
            case SERVER_ERROR -> 500;
            case EDGE -> findSuccessStatus(endpoint);
        };
    }

    private int findSuccessStatus(Endpoint endpoint) {
        if (endpoint.getResponses() == null) {
            return 200;
        }

        return endpoint.getResponses().keySet().stream()
                .filter(status -> !status.equals("default"))
                .map(Integer::parseInt)
                .filter(status -> status >= 200 && status < 300)
                .findFirst()
                .orElse(200);
    }

    private Map<String, Object> buildRequestContext(Endpoint endpoint, HttpServletRequest request, String body) {
        Map<String, Object> context = new HashMap<>();
        
        context.put("method", request.getMethod());
        context.put("path", request.getRequestURI());
        context.put("operationId", endpoint.getOperationId());
        context.put("summary", endpoint.getSummary());
        
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            context.put("queryString", request.getQueryString());
        }
        
        Map<String, String> headers = new HashMap<>();
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            if (!key.startsWith("X-Mock-")) {
                headers.put(key, request.getHeader(key));
            }
        }
        if (!headers.isEmpty()) {
            context.put("headers", headers);
        }
        
        if (body != null && !body.isEmpty()) {
            try {
                context.put("requestBody", objectMapper.readValue(body, Map.class));
            } catch (Exception e) {
                context.put("requestBody", body);
            }
        }
        
        if (endpoint.getParameters() != null && !endpoint.getParameters().isEmpty()) {
            context.put("parameters", endpoint.getParameters());
        }
        
        return context;
    }
}