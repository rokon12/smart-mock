package ca.bazlur.smartmock.llm;

import ca.bazlur.smartmock.model.Plan;
import ca.bazlur.smartmock.model.Scenario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {
    private static final String NL = "\n";

    private final ObjectMapper objectMapper;

    public String buildGenerationPrompt(@NonNull Plan plan) {
        final var sb = new StringBuilder(2048);

        sb.append("""
            You are a strict JSON generator for API mocks.

            Given:
            - OpenAPI schema for the response
            - Request context (method, path, params, sample inputs)
            """);

        sb.append("- Scenario: ").append(valueOf(plan.getScenario())).append(NL);
        sb.append("- Status code: ").append(plan.getStatusCode()).append(NL).append(NL);

        sb.append("""
            Rules:
            - Output ONLY valid JSON. No commentary, no markdown, no explanations.
            - The JSON must be valid according to the provided JSON Schema.
            - Respect field types, enums, and formats (email, uuid, date-time).
            - Use realistic values conditioned on input when obvious.
            - For arrays, produce 1-3 items unless a page size is specified.
            """);

        sb.append(scenarioRule(plan.getScenario())).append(NL);

        if (plan.getJsonSchema() != null && !plan.getJsonSchema().isBlank()) {
            appendSection(sb, "JSON Schema", plan.getJsonSchema());
        }

        if (plan.getRequestContext() != null) {
            appendSection(sb, "Request Context", toJson(plan.getRequestContext()));
        }

        sb.append("Generate the JSON response now:");

        return sb.toString();
    }

    public String buildRepairPrompt(@NonNull String invalidJson, @NonNull String validationError) {
        return String.format("""
            You are a strict JSON fixer. Output ONLY valid JSON. No commentary, no markdown, no explanations.

            Invalid JSON:
            %s

            Validation error:
            %s

            Requirements:
            - Return a syntactically valid JSON document.
            - Preserve the original structure and fields where possible.
            - If required fields are missing, add minimal valid values.
            - Use realistic sample values that satisfy types/enums/formats.

            Return ONLY the corrected JSON:
            """,
            invalidJson.trim(),
            validationError.trim()
        );
    }

    private static String scenarioRule(Scenario scenario) {
        if (scenario == null) return "- Generate realistic, successful response data." + NL;

        return switch (scenario) {
            case HAPPY      -> "- Generate realistic, successful response data." + NL;
            case EDGE       -> "- Use boundary values (min/max, empty arrays, very long/empty strings)." + NL;
            case INVALID    -> "- Generate a validation error response consistent with the schema's error format." + NL;
            case RATE_LIMIT -> "- Generate a rate limit error with retry information (retry-after seconds/ISO timestamp)." + NL;
            case SERVER_ERROR -> "- Generate a server error with a trace id/correlation id." + NL;
        };
    }

    private static void appendSection(StringBuilder sb, String title, String content) {
        sb.append(title).append(":").append(NL)
            .append(content).append(NL).append(NL);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize request context to JSON; falling back to toString()", e);
            return String.valueOf(value);
        }
    }

    private static String valueOf(Object o) {
        return o == null ? "HAPPY" : o.toString();
    }
}
