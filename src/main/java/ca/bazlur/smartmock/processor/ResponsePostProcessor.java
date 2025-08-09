package ca.bazlur.smartmock.processor;

import ca.bazlur.smartmock.model.MockResult;
import ca.bazlur.smartmock.model.Plan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponsePostProcessor {
    private final ObjectMapper objectMapper;

    public MockResult process(String jsonResponse, Plan plan, HttpServletRequest request) {
        try {
            String processedJson = jsonResponse;
            
            String seed = request.getHeader("X-Mock-Seed");
            if (seed != null) {
                processedJson = applySeed(processedJson, seed);
            }
            
            String temperature = request.getHeader("X-Mock-Temperature");
            if (temperature != null) {
                try {
                    double temp = Double.parseDouble(temperature);
                    processedJson = applyTemperature(processedJson, temp);
                } catch (NumberFormatException e) {
                    log.warn("Invalid temperature value: {}", temperature);
                }
            }
            
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Content-Type", "application/json");
            responseHeaders.add("X-Mock-Scenario", plan.getScenario().getValue());
            responseHeaders.add("X-Mock-Generated", "true");
            
            if (plan.getScenario().getValue().equals("rate-limit")) {
                responseHeaders.add("Retry-After", "60");
                responseHeaders.add("X-RateLimit-Limit", "100");
                responseHeaders.add("X-RateLimit-Remaining", "0");
                responseHeaders.add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + 60));
            }
            
            if (plan.getScenario().getValue().equals("server-error")) {
                responseHeaders.add("X-Trace-Id", UUID.randomUUID().toString());
            }
            
            return MockResult.builder()
                    .status(plan.getStatusCode())
                    .body(processedJson)
                    .headers(responseHeaders)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error post-processing response", e);
            return MockResult.builder()
                    .status(plan.getStatusCode())
                    .body(jsonResponse)
                    .headers(new HttpHeaders())
                    .build();
        }
    }

    private String applySeed(String json, String seed) {
        try {
            long seedValue = seed.hashCode();
            Random random = new Random(seedValue);
            
            JsonNode node = objectMapper.readTree(json);
            JsonNode processed = processNodeWithSeed(node, random);
            
            return objectMapper.writeValueAsString(processed);
        } catch (Exception e) {
            log.error("Error applying seed", e);
            return json;
        }
    }

    private JsonNode processNodeWithSeed(JsonNode node, Random random) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node.deepCopy();
            objectNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                
                if (key.toLowerCase().contains("id") && value.isNumber()) {
                    objectNode.put(key, Math.abs(random.nextInt(10000)));
                } else if (key.toLowerCase().contains("timestamp") && value.isNumber()) {
                    objectNode.put(key, System.currentTimeMillis() - random.nextInt(86400000));
                } else if (value.isObject() || value.isArray()) {
                    objectNode.set(key, processNodeWithSeed(value, random));
                }
            });
            return objectNode;
        } else if (node.isArray()) {
            return node;
        }
        
        return node;
    }

    private String applyTemperature(String json, double temperature) {
        double clampedTemp = Math.max(0, Math.min(1, temperature));
        log.debug("Applying temperature: {}", clampedTemp);
        return json;
    }
}