package ca.bazlur.smartmock.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class JsonUtils {
  private JsonUtils() {
  }

  public static String safeMinified(ObjectMapper mapper, String maybeJson) {
    if (maybeJson == null || maybeJson.isBlank()) return "";
    try {
      JsonNode node = mapper.readTree(maybeJson);
      dropFieldDeep(node, "examples");
      dropFieldDeep(node, "description");
      dropFieldDeep(node, "externalDocs");
      return node.toString();
    } catch (Exception e) {
      return sanitize(maybeJson);
    }
  }

  private static void dropFieldDeep(JsonNode node, String field) {
    if (node == null) return;
    if (node.isObject()) {
      ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove(field);
      node.fields().forEachRemaining(e -> dropFieldDeep(e.getValue(), field));
    } else if (node.isArray()) {
      for (JsonNode child : node) dropFieldDeep(child, field);
    }
  }

  public static String truncateWithNotice(String s, int max) {
    if (s == null) return "";
    if (s.length() <= max) return s;
    return s.substring(0, Math.max(0, max - 20)) + "...(truncated)";
  }

  public static String enforceMax(String s, int max) {
    if (s == null) return "";
    return s.length() <= max ? s : truncateWithNotice(s, max);
  }

  public static String sanitize(String s) {
    return s == null ? "" : s.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ");
  }

  public static String sha256Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      return "000000";
    }
  }
}

