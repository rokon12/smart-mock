package ca.bazlur.smartmock.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.media.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonSchemaConverter {
  private final ObjectMapper mapper;

  public String convertToJsonSchema(Schema<?> openApiSchema) {
    try {
      ObjectNode root = mapper.createObjectNode();
      root.put("$schema", "http://json-schema.org/draft-07/schema#");
      ObjectNode converted = convert(openApiSchema);
      if (converted != null) root.setAll(converted);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    } catch (Exception e) {
      log.error("Error converting OpenAPI schema to JSON Schema", e);
      return "{}";
    }
  }

  private ObjectNode convert(Schema<?> s) {
    if (s == null) return null;

    // $ref shortcut
    if (s.get$ref() != null) {
      ObjectNode refNode = mapper.createObjectNode();
      refNode.put("$ref", s.get$ref()); // keep OAS component refs; adjust if you relocate components
      return refNode;
    }

    ObjectNode n = mapper.createObjectNode();

    // common keywords
    putIfNotBlank(n, "title", s.getTitle());
    putIfNotBlank(n, "description", s.getDescription());
    putIfNotBlank(n, "format", s.getFormat());
    putIfNotNull(n, "default", mapper.valueToTree(s.getDefault()));
    putIfNotNull(n, "example", mapper.valueToTree(s.getExample()));
    putIfTrue(n, "deprecated", Boolean.TRUE.equals(s.getDeprecated()));
    putIfTrue(n, "readOnly", Boolean.TRUE.equals(s.getReadOnly()));
    putIfTrue(n, "writeOnly", Boolean.TRUE.equals(s.getWriteOnly()));
    if (s.getMultipleOf() != null) n.put("multipleOf", s.getMultipleOf());

    // enums
    if (s.getEnum() != null && !s.getEnum().isEmpty()) {
      ArrayNode enums = n.putArray("enum");
      s.getEnum().forEach(v -> enums.add(mapper.valueToTree(v)));
    }

    // composition
    if (s instanceof ComposedSchema cs) {
      copyList(cs.getAllOf(), n.putArray("allOf"));
      copyList(cs.getAnyOf(), n.putArray("anyOf"));
      copyList(cs.getOneOf(), n.putArray("oneOf"));
      if (cs.getNot() != null) n.set("not", convert(cs.getNot()));
      // still allow fallthrough to capture type/nullable if present on wrapper
    }

    // type & nullable handling
    String type = s.getType();
    if (type != null) {
      if (Boolean.TRUE.equals(s.getNullable())) {
        ArrayNode t = n.putArray("type");
        t.add(type);
        t.add("null");
      } else {
        n.put("type", type);
      }
    } else if (Boolean.TRUE.equals(s.getNullable())) {
      ArrayNode t = n.putArray("type");
      t.add("null");
    }

    // numbers
    if (s.getMinimum() != null) n.put("minimum", s.getMinimum());
    if (s.getMaximum() != null) n.put("maximum", s.getMaximum());
    // OAS booleans -> draft-07 numeric exclusives
    if (Boolean.TRUE.equals(s.getExclusiveMinimum()) && s.getMinimum() != null) {
      n.remove("minimum");
      n.put("exclusiveMinimum", s.getMinimum());
    }
    if (Boolean.TRUE.equals(s.getExclusiveMaximum()) && s.getMaximum() != null) {
      n.remove("maximum");
      n.put("exclusiveMaximum", s.getMaximum());
    }

    // strings
    if (s.getMinLength() != null) n.put("minLength", s.getMinLength());
    if (s.getMaxLength() != null) n.put("maxLength", s.getMaxLength());
    if (s.getPattern() != null) n.put("pattern", s.getPattern());

    // arrays
    if ("array".equals(type)) {
      if (s.getItems() != null) n.set("items", convert(s.getItems()));
      if (s.getMinItems() != null) n.put("minItems", s.getMinItems());
      if (s.getMaxItems() != null) n.put("maxItems", s.getMaxItems());
      if (s.getUniqueItems() != null) n.put("uniqueItems", s.getUniqueItems());
    }

    // objects & maps
    if ("object".equals(type) || s instanceof ObjectSchema || s instanceof MapSchema) {
      ObjectNode props = n.putObject("properties");
      if (s.getProperties() != null) {
        for (Map.Entry<String, Schema> e : s.getProperties().entrySet()) {
          props.set(e.getKey(), convert(e.getValue()));
        }
      }
      if (s.getRequired() != null && !s.getRequired().isEmpty()) {
        ArrayNode req = n.putArray("required");
        s.getRequired().forEach(req::add);
      }

      // additionalProperties: boolean or schema
      Object addl = s.getAdditionalProperties();
      if (addl instanceof Boolean b) {
        n.put("additionalProperties", b);
      } else if (addl instanceof Schema<?> as) {
        n.set("additionalProperties", convert(as));
      } else if (s instanceof MapSchema ms) {
        Object ap = ms.getAdditionalProperties();
        if (ap instanceof Boolean b2) n.put("additionalProperties", b2);
        else if (ap instanceof Schema<?> aps)
          n.set("additionalProperties", convert(aps));
      }
    }

    return n;
  }

  private void copyList(List<Schema> list, ArrayNode target) {
    if (list == null || list.isEmpty()) return;
    list.forEach(s -> target.add(convert(s)));
  }

  private void putIfNotBlank(ObjectNode n, String k, String v) {
    if (v != null && !v.isBlank()) n.put(k, v);
  }

  private void putIfNotNull(ObjectNode n, String k, com.fasterxml.jackson.databind.JsonNode v) {
    if (v != null && !v.isNull()) n.set(k, v);
  }

  private void putIfTrue(ObjectNode n, String k, boolean v) {
    if (v) n.put(k, true);
  }
}
