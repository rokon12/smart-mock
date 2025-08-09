package ca.bazlur.smartmock.service;

import ca.bazlur.smartmock.model.MockResult;
import ca.bazlur.smartmock.model.Plan;
import ca.bazlur.smartmock.model.Scenario;
import ca.bazlur.smartmock.model.Signature;
import ca.bazlur.smartmock.openapi.Endpoint;
import ca.bazlur.smartmock.openapi.OpenApiIndex;
import ca.bazlur.smartmock.planner.ResponsePlanner;
import ca.bazlur.smartmock.llm.LlmRunner;
import ca.bazlur.smartmock.validation.JsonValidator;
import ca.bazlur.smartmock.processor.ResponsePostProcessor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.language.LanguageModel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MockService {
    private final OpenApiIndex openApiIndex;
    private final LanguageModel chatModel;
    private final ResponsePlanner planner;
    private final LlmRunner llmRunner;
    private final JsonValidator validator;
    private final ResponsePostProcessor postProcessor;
    private final Cache<Signature, MockResult> cache;

    public MockService(OpenApiIndex openApiIndex,
                       LanguageModel chatModel,
                       ResponsePlanner planner,
                       LlmRunner llmRunner,
                       JsonValidator validator,
                       ResponsePostProcessor postProcessor) {
        this.openApiIndex = openApiIndex;
        this.chatModel = chatModel;
        this.planner = planner;
        this.llmRunner = llmRunner;
        this.validator = validator;
        this.postProcessor = postProcessor;
        this.cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build();
    }

    public MockResult generate(HttpServletRequest request, String body) {
        String path = request.getRequestURI().replace("/mock", "");
        String method = request.getMethod();
        
        Signature signature = Signature.from(request, body);
        
        MockResult cached = cache.getIfPresent(signature);
        if (cached != null) {
            log.debug("Cache hit for signature: {}", signature);
            applyLatency(request);
            return cached;
        }

        try {
            Endpoint endpoint = openApiIndex.match(method, path)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "No matching endpoint found in OpenAPI spec for " + method + " " + path));

            Scenario scenario = Scenario.fromHeaders(request);
            Plan plan = planner.plan(endpoint, scenario, request, body);
            
            String jsonResponse = llmRunner.generateResponse(chatModel, plan);
            
            try {
                jsonResponse = validator.validate(jsonResponse);
            } catch (JsonValidator.ValidationException e) {
                log.warn("Validation failed, attempting repair: {}", e.getMessage());
                jsonResponse = llmRunner.repairResponse(chatModel, jsonResponse, e.getMessage());
            }
            
            MockResult result = postProcessor.process(jsonResponse, plan, request);
            cache.put(signature, result);
            
            applyLatency(request);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error generating mock response", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error generating mock response: " + e.getMessage(), e);
        }
    }

    private void applyLatency(HttpServletRequest request) {
        String latencyHeader = request.getHeader("X-Mock-Latency");
        if (latencyHeader != null) {
            try {
                Duration latency = Duration.parse("PT" + latencyHeader.toUpperCase());
                log.debug("Applying latency: {}", latency);
                Thread.sleep(latency.toMillis());
            } catch (Exception e) {
                log.warn("Invalid latency format or interruption: {}", latencyHeader);
            }
        }
    }
}