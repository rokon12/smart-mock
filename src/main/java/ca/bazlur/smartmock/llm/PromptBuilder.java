package ca.bazlur.smartmock.llm;

import ca.bazlur.smartmock.model.Plan;
import ca.bazlur.smartmock.model.Scenario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

  private static final String NL = "\n";
  private static final int DEFAULT_MAX_CHARS = 16000; // ~4k tokens rough guide
  private static final int MAX_SCHEMA_CHARS = 6000;
  private static final int MAX_CTX_CHARS = 4000;

  private final ObjectMapper objectMapper;
  private final ContextRegistry contextRegistry;

  public String buildGenerationPrompt(@NonNull Plan plan) {
    final Scenario scenario = plan.getScenario() == null ? Scenario.HAPPY : plan.getScenario();

    final String endpointPath = extractPath(plan);
    final String method = extractString(plan.getRequestContext(), "method");
    final String operationId = extractString(plan.getRequestContext(), "operationId");

    final String minifiedSchema = JsonUtils.safeMinified(objectMapper, plan.getJsonSchema());
    final String requestCtx = toStableMinifiedJson(plan.getRequestContext());

    final EndpointInfo info = new EndpointInfo(
        sanitize(endpointPath),
        sanitize(operationId),
        sanitize(method),
        JsonUtils.truncateWithNotice(minifiedSchema, MAX_SCHEMA_CHARS),
        JsonUtils.truncateWithNotice(requestCtx, MAX_CTX_CHARS)
    );

    final List<ContextBlock> chosenBlocks = contextRegistry.select(
        info,
        /*maxBlocks*/ 2,
        /*minScore*/ 0.25,
        /*budgetChars*/ 3000
    );

    StringBuilder sb = new StringBuilder(4096);
    sb.append(HEAD_INTRO);

    sb.append("- Scenario: ").append(scenario).append(NL);
    sb.append("- Status code: ").append(plan.getStatusCode()).append(NL);
    sb.append("- Endpoint: ").append(endpointPath).append(NL).append(NL);

    for (ContextBlock b : chosenBlocks) {
      sb.append(b.render(info)).append(NL);
    }

    sb.append(STRICT_RULES).append(NL);
    sb.append(scenarioDelta(scenario)).append(NL).append(NL);

    if (!info.jsonSchemaMinified().isBlank()) {
      appendSection(sb, "JSON Schema", info.jsonSchemaMinified());
    }
    if (!info.requestContextMinified().isBlank()) {
      appendSection(sb, "Request Context", info.requestContextMinified());
    }

    sb.append(FINAL_REMINDER);

    String prompt = JsonUtils.enforceMax(sb.toString(), DEFAULT_MAX_CHARS);

    logPromptStats(prompt, chosenBlocks);
    return prompt;
  }

  public String buildRepairPrompt(@NonNull String invalidJson, @NonNull String validationError) {
    return String.format(REPAIR_TEMPLATE,
        JsonUtils.truncateWithNotice(invalidJson.trim(), 6000),
        JsonUtils.truncateWithNotice(validationError.trim(), 2000));
  }

  private static String sanitize(String s) {
    return s == null ? "" : s.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ");
  }

  private static void appendSection(StringBuilder sb, String title, String content) {
    sb.append(title).append(":").append(NL).append(content).append(NL).append(NL);
  }

  private String toStableMinifiedJson(Object value) {
    if (value == null) return "";
    try {
      ObjectMapper stable = JsonMapper.builder()
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .build();
      return sanitize(stable.writeValueAsString(value));
    } catch (JsonProcessingException e) {
      log.warn("Request context serialization failed, falling back to toString()", e);
      return sanitize(String.valueOf(value));
    }
  }

  private static String extractPath(Plan plan) {
    Object ctx = plan.getRequestContext();
    if (ctx instanceof Map<?, ?> m) {
      Object path = m.get("path");
      if (path != null) return String.valueOf(path);
    }
    return "";
  }

  private static String extractString(Object ctx, String key) {
    if (ctx instanceof Map<?, ?> m) {
      Object v = m.get(key);
      if (v != null) return String.valueOf(v);
    }
    return "";
  }

  private void logPromptStats(String prompt, List<ContextBlock> blocks) {
    int chars = prompt.length();
    int bytes = prompt.getBytes(StandardCharsets.UTF_8).length;
    String sha = JsonUtils.sha256Hex(prompt);
    String ids = blocks.stream().map(ContextBlock::id).reduce((a, b) -> a + "," + b).orElse("-");
    log.debug("Prompt size: {} chars, {} bytes, sha256={}, blocks=[{}], ts={}",
        chars, bytes, sha, ids, Instant.now());
  }

  private static String scenarioDelta(Scenario s) {
    return switch (s) {
      case HAPPY ->
          "- Generate realistic, successful response data with diverse, believable values (no generic names).";
      case EDGE ->
          "- Use boundary values (min/max, empty arrays, very long but realistic strings) while keeping names realistic.";
      case INVALID ->
          "- Generate a validation error response with specific field errors and helpful messages.";
      case RATE_LIMIT ->
          "- Generate a rate limit error with a Retry-After header (e.g., 60 seconds) and a clear message.";
      case SERVER_ERROR ->
          "- Generate a server error with a trace id (e.g., 'trace-id: 7f3a2b1c-4d5e-6f7a-8b9c-0d1e2f3a4b5c').";
    };
  }

  private static final String HEAD_INTRO = """
      You are a strict JSON generator for API mocks that MUST generate realistic data.
      
      CRITICAL: You are FORBIDDEN from using generic placeholder names like:
      - "Product 1", "Product 2", "Product 3"
      - "This is product 1", "Description 1"
      - "Item 1", "Test", "Sample", "Example"
      
      YOU MUST GENERATE REALISTIC DATA!
      """;

  private static final String STRICT_RULES = """
      STRICT RULES:
      1. Output ONLY valid JSON matching the schema
      2. ABSOLUTELY NO GENERIC NAMES - Each item must be unique and realistic
      3. For name fields: Use ACTUAL product/person/company names
      4. For description fields: Write MEANINGFUL, UNIQUE descriptions
      5. For price fields: Use varied, realistic prices ($19.99, $249.00, $1,299.99)
      6. For arrays: Generate 5-10 COMPLETELY DIFFERENT items
      
      If you generate "Product 1" or similar generic names, you have FAILED.
      """;

  private static final String FINAL_REMINDER = """
      FINAL REMINDER: Generate REALISTIC, DIVERSE data.
      NO "Product 1", NO "Item 2", NO generic placeholders!
      Each item must have a UNIQUE, REALISTIC name and description.
      
      Generate the JSON response now:
      """;

  private static final String REPAIR_TEMPLATE = """
      You are a strict JSON fixer. Output ONLY valid JSON. No commentary, no markdown, no explanations.
      
      Invalid JSON:
      %s
      
      Validation error:
      %s
      
      Requirements:
      - Return a syntactically valid JSON document.
      - Preserve the original structure and fields where possible.
      - If required fields are missing, add realistic valid values.
      - NEVER use generic placeholders like "string1", "test", "sample".
      - Use proper names like "MacBook Pro 16", "John Smith", "Microsoft Corporation".
      
      Return ONLY the corrected JSON:
      """;
}
