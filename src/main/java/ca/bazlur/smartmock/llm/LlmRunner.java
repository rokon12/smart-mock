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
        // First try to find a complete JSON array
        int arrayStart = text.indexOf('[');
        int arrayEnd = text.lastIndexOf(']');
        
        if (arrayStart != -1 && arrayEnd != -1 && arrayStart < arrayEnd) {
            return text.substring(arrayStart, arrayEnd + 1);
        }
        
        // Then try to find a complete JSON object
        int objStart = text.indexOf('{');
        int objEnd = text.lastIndexOf('}');
        
        if (objStart != -1 && objEnd != -1 && objStart < objEnd) {
            String extracted = text.substring(objStart, objEnd + 1);
            
            // Check if this looks like array items without array wrapper
            // If we have multiple objects separated by commas, wrap them in an array
            if (extracted.contains("},") && !extracted.startsWith("[")) {
                // Count opening braces to see if we have multiple objects
                int braceCount = 0;
                boolean inString = false;
                char prevChar = ' ';
                
                for (char c : extracted.toCharArray()) {
                    if (c == '"' && prevChar != '\\') {
                        inString = !inString;
                    } else if (!inString && c == '{') {
                        braceCount++;
                    }
                    prevChar = c;
                }
                
                // If we have multiple objects, wrap them in an array
                if (braceCount > 1) {
                    return "[" + extracted + "]";
                }
            }
            
            return extracted;
        }
        
        return "{}";
    }
}