package ca.bazlur.smartmock.llm;

import ca.bazlur.smartmock.model.Plan;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.language.LanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmRunner {
    private final ObjectMapper objectMapper;
    private final PromptBuilder promptBuilder;

    public String generateResponse(LanguageModel model, Plan plan) {
        String prompt = promptBuilder.buildGenerationPrompt(plan);
        log.debug("Generating response with prompt length: {}", prompt.length());
        
        var response = model.generate(prompt);
        log.debug("Generated response: {}", response);
        String content = response.content();

        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        content = content.trim();
        
        try {
            objectMapper.readTree(content);
            return content;
        } catch (Exception e) {
            log.warn("LLM response is not valid JSON, attempting to extract JSON");
            return extractJson(content);
        }
    }

    public String repairResponse(LanguageModel model, String invalidJson, String validationError) {
        String prompt = promptBuilder.buildRepairPrompt(invalidJson, validationError);
        log.debug("Repairing response with validation error: {}", validationError);
        
        String response = model.generate(prompt).content();
        
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        
        return response.trim();
    }

    private String extractJson(String text) {
        int startIndex = text.indexOf('{');
        int endIndex = text.lastIndexOf('}');
        
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            startIndex = text.indexOf('[');
            endIndex = text.lastIndexOf(']');
        }
        
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return text.substring(startIndex, endIndex + 1);
        }
        
        return "{}";
    }
}