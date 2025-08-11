package ca.bazlur.smartmock.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestResponseCorrelator {
    
    private final ObjectMapper objectMapper;
    
    public String generateCorrelations(Object requestContextObj) {
        Map<String, Object> requestContext = convertToMap(requestContextObj);
        return generateCorrelations(requestContext);
    }
    
    public String generateCorrelations(Map<String, Object> requestContext) {
        if (requestContext == null || requestContext.isEmpty()) {
            return "";
        }
        
        List<String> correlations = new ArrayList<>();
        
        String path = extractString(requestContext, "path");
        String method = extractString(requestContext, "method");
        Map<String, Object> pathParams = extractMap(requestContext, "pathParameters");
        Map<String, Object> queryParams = extractMap(requestContext, "queryParameters");
        Object requestBody = requestContext.get("body");
        
        if (pathParams != null && !pathParams.isEmpty()) {
            correlatePathParameters(pathParams, correlations);
        }
        
        if (queryParams != null && !queryParams.isEmpty()) {
            correlateQueryParameters(queryParams, correlations);
        }
        
        if (requestBody != null) {
            correlateRequestBody(requestBody, method, correlations);
        }
        
        String resourceId = extractIdFromPath(path);
        if (resourceId != null) {
            correlations.add(String.format("The response MUST reference ID '%s' from the request path", resourceId));
            correlations.add(String.format("If returning a single resource, its ID field MUST be '%s'", resourceId));
        }
        
        if (correlations.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("\nREQUEST-RESPONSE CORRELATIONS:\n");
        result.append("Your response MUST respect these relationships from the request:\n");
        for (String correlation : correlations) {
            result.append("- ").append(correlation).append("\n");
        }
        result.append("\n");
        
        return result.toString();
    }
    
    private void correlatePathParameters(Map<String, Object> pathParams, List<String> correlations) {
        for (Map.Entry<String, Object> entry : pathParams.entrySet()) {
            String param = entry.getKey();
            Object value = entry.getValue();
            
            if (param.toLowerCase().contains("id")) {
                correlations.add(String.format("Resource ID must be '%s'", value));
            } else if (param.toLowerCase().contains("category")) {
                correlations.add(String.format("All items must belong to category '%s'", value));
            } else if (param.toLowerCase().contains("user")) {
                correlations.add(String.format("Data must be associated with user '%s'", value));
            } else if (param.toLowerCase().contains("type")) {
                correlations.add(String.format("All items must be of type '%s'", value));
            } else {
                correlations.add(String.format("Response must be filtered by %s='%s'", param, value));
            }
        }
    }
    
    private void correlateQueryParameters(Map<String, Object> queryParams, List<String> correlations) {
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            String param = entry.getKey();
            Object value = entry.getValue();
            
            if (param.equals("page") || param.equals("offset")) {
                correlations.add(String.format("Response should reflect pagination: %s=%s", param, value));
            } else if (param.equals("limit") || param.equals("size") || param.equals("count")) {
                int limit = parseInteger(value, 10);
                correlations.add(String.format("Return exactly %d items (or fewer if less available)", limit));
            }
            else if (param.equals("search") || param.equals("q") || param.equals("query")) {
                correlations.add(String.format("All items must be relevant to search term '%s'", value));
                correlations.add(String.format("Item names/descriptions should contain or relate to '%s'", value));
            } else if (param.equals("sort") || param.equals("orderBy")) {
                correlations.add(String.format("Items must be sorted by %s", value));
            } else if (param.equals("filter") || param.startsWith("filter.")) {
                correlations.add(String.format("Apply filter: %s=%s", param, value));
            }
            else if (param.contains("from") || param.contains("start")) {
                correlations.add(String.format("All dates must be after %s", value));
            } else if (param.contains("to") || param.contains("end")) {
                correlations.add(String.format("All dates must be before %s", value));
            }
            else if (param.contains("min") && (param.contains("price") || param.contains("amount"))) {
                correlations.add(String.format("All prices must be >= %s", value));
            } else if (param.contains("max") && (param.contains("price") || param.contains("amount"))) {
                correlations.add(String.format("All prices must be <= %s", value));
            }
            else if (param.equals("status") || param.equals("state")) {
                correlations.add(String.format("All items must have status '%s'", value));
            }
            else if (value instanceof Boolean) {
                if ((Boolean) value) {
                    correlations.add(String.format("Only include items where %s is true", param));
                } else {
                    correlations.add(String.format("Exclude items where %s is true", param));
                }
            }
        }
    }
    
    private void correlateRequestBody(Object requestBody, String method, List<String> correlations) {
        if (requestBody == null) return;
        
        try {
            JsonNode bodyNode;
            if (requestBody instanceof String) {
                bodyNode = objectMapper.readTree((String) requestBody);
            } else {
                bodyNode = objectMapper.valueToTree(requestBody);
            }
            
            if (method.equalsIgnoreCase("POST")) {
                correlations.add("Response must confirm creation of the resource with data from request body");
                
                if (bodyNode.has("name")) {
                    correlations.add(String.format("Created resource must have name '%s'", bodyNode.get("name").asText()));
                }
                if (bodyNode.has("email")) {
                    correlations.add(String.format("Created resource must have email '%s'", bodyNode.get("email").asText()));
                }
                if (bodyNode.has("title")) {
                    correlations.add(String.format("Created resource must have title '%s'", bodyNode.get("title").asText()));
                }
                
                if (bodyNode.has("category")) {
                    correlations.add(String.format("Created resource must be in category '%s'", bodyNode.get("category").asText()));
                }
                if (bodyNode.has("type")) {
                    correlations.add(String.format("Created resource must be of type '%s'", bodyNode.get("type").asText()));
                }
            } else if (method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH")) {
                correlations.add("Response must reflect the updates from request body");
                correlations.add("Updated fields must match values provided in request");
                correlations.add("Include an 'updatedAt' timestamp with current time");
            }
            
            if (bodyNode.has("search") || bodyNode.has("query")) {
                String searchTerm = bodyNode.has("search") ? 
                    bodyNode.get("search").asText() : bodyNode.get("query").asText();
                correlations.add(String.format("Results must be relevant to '%s'", searchTerm));
            }
            
            if (bodyNode.isArray()) {
                correlations.add(String.format("Process all %d items from request", bodyNode.size()));
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse request body for correlations", e);
        }
    }
    
    private String extractIdFromPath(String path) {
        if (path == null) return null;
        
        Pattern[] patterns = {
            Pattern.compile("/([a-zA-Z0-9-]+)$"),  // /users/123
            Pattern.compile("/([a-zA-Z0-9-]+)/"),   // /users/123/orders
            Pattern.compile("/(\\d+)"),             // /products/456
            Pattern.compile("/([a-f0-9]{24})"),     // MongoDB ObjectId
            Pattern.compile("/([a-f0-9-]{36})")     // UUID
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                String id = matcher.group(1);
                if (!isCommonPathSegment(id)) {
                    return id;
                }
            }
        }
        
        return null;
    }
    
    private boolean isCommonPathSegment(String segment) {
        String[] commonSegments = {
            "api", "v1", "v2", "v3", "users", "products", "orders",
            "items", "customers", "accounts", "admin", "public"
        };
        
        for (String common : commonSegments) {
            if (common.equalsIgnoreCase(segment)) {
                return true;
            }
        }
        return false;
    }
    
    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
    
    private int parseInteger(Object value, int defaultValue) {
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj == null) return new HashMap<>();
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        try {
            return objectMapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            log.warn("Failed to convert request context to map", e);
            return new HashMap<>();
        }
    }
}