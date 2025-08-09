package ca.bazlur.smartmock.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonValidator {
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    public String validate(String jsonString) throws ValidationException {
        try {
            objectMapper.readTree(jsonString);
            return jsonString;
        } catch (Exception e) {
            String errorMsg = "Invalid JSON: " + e.getMessage();
            log.error(errorMsg);
            throw new ValidationException(errorMsg);
        }
    }

    public String validateAgainstSchema(String jsonString, String schemaString) throws ValidationException {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode schemaNode = objectMapper.readTree(schemaString);
            
            JsonSchema schema = schemaFactory.getSchema(schemaNode);
            Set<ValidationMessage> errors = schema.validate(jsonNode);
            
            if (!errors.isEmpty()) {
                String errorMsg = errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining(", "));
                log.warn("JSON Schema validation failed: {}", errorMsg);
                throw new ValidationException(errorMsg);
            }
            
            return jsonString;
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            log.error("Error during validation", e);
            return jsonString;
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}