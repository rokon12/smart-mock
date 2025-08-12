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
  private final RequestResponseCorrelator requestResponseCorrelator;
  private final FieldSemantics fieldSemantics;

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
    sb.append("- Endpoint: ").append(endpointPath).append(NL);
    
    // Extract and emphasize size parameter
    if (plan.getRequestContext() instanceof Map<?, ?> ctx) {
      Object queryObj = ctx.get("query");
      if (queryObj instanceof Map<?, ?> queryParams) {
        Object sizeParam = queryParams.get("size");
        if (sizeParam != null) {
          sb.append("- CRITICAL: The response MUST contain EXACTLY ").append(sizeParam).append(" items in the array!").append(NL);
        }
      }
    }
    
    // Add seed information if present
    Object seed = plan.getRequestContext().get("seed");
    if (seed != null) {
      log.info("Including seed in prompt: {}", seed);
      int seedHash = Math.abs(seed.toString().hashCode());
      int startId = (seedHash % 900) + 100; // 100-999 range
      sb.append("- Random seed: ").append(seed).append(NL);
      sb.append("- CRITICAL: Use seed hash ").append(seedHash).append(" for variation").append(NL);
      sb.append("- MANDATORY: Start numeric IDs from ").append(startId).append(NL);
      sb.append("- Use seed to vary the data systematically").append(NL);
      sb.append("- Different seeds MUST produce completely different data").append(NL);
    } else {
      log.debug("No seed found in request context");
    }
    sb.append(NL);

    String correlations = requestResponseCorrelator.generateCorrelations(plan.getRequestContext());
    if (!correlations.isBlank()) {
      sb.append(correlations).append(NL);
    }

    for (ContextBlock b : chosenBlocks) {
      sb.append(b.render(info)).append(NL);
    }

    sb.append(STRICT_RULES).append(NL);
    sb.append(scenarioDelta(scenario)).append(NL).append(NL);

    String fieldGuidance = fieldSemantics.analyzeSchema(info.jsonSchemaMinified());
    if (!fieldGuidance.isBlank()) {
      sb.append(fieldGuidance).append(NL);
    }

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
    String semanticHints = "";
    if (validationError.toLowerCase().contains("field") || validationError.toLowerCase().contains("property")) {
      semanticHints = "\nUse semantic understanding: emails must be valid, dates in ISO format, prices as numbers, etc.\n";
    }
    
    return String.format(REPAIR_TEMPLATE,
        JsonUtils.truncateWithNotice(invalidJson.trim(), 6000),
        JsonUtils.truncateWithNotice(validationError.trim(), 2000)) + semanticHints;
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
      You are a strict JSON generator for API mocks that MUST follow the provided JSON schema EXACTLY.
      
      CRITICAL RULES:
      1. You MUST generate data that matches the JSON schema structure and field names
      2. You MUST respect the data types specified in the schema
      3. You MUST follow enum values if specified in the schema
      4. You are FORBIDDEN from using generic placeholder names
      
      Generate realistic, schema-compliant data!
      """;

  private static final String STRICT_RULES = """
      STRICT RULES:
      1. Output ONLY valid JSON matching the EXACT schema provided
      2. Use the EXACT field names from the schema (not generic field names)
      3. Follow the data types specified in the schema EXACTLY
      4. If the schema has enum values, use ONLY those values
      5. Generate realistic data appropriate for the schema context:
         - For pets: Use real pet names like "Buddy", "Max", "Luna"
         - For users: Use real person names
         - For products: Use real product names
      6. For arrays: Check the 'size' or 'limit' parameter from the request
         - If limit=10, generate EXACTLY 10 items
         - Each item MUST be unique and realistic
      
      FOLLOW THE SCHEMA - do not generate unrelated data types!
      """;

  private static final String FINAL_REMINDER = """
      FINAL REMINDER: 
      1. Follow the JSON schema EXACTLY - use the correct field names and types
      2. Generate data appropriate for the schema context (pets, users, products, etc.)
      3. Make each item unique and realistic
      
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
