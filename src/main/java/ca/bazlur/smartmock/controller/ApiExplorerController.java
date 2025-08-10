package ca.bazlur.smartmock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import ca.bazlur.smartmock.service.SchemaManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ApiExplorerController {
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
  };
  private final SchemaManager schemaManager;
  private final ObjectMapper objectMapper;
  private final YAMLMapper yamlMapper;

  @GetMapping("/explorer")
  public String redirectToSwagger() {
    return "redirect:/swagger-ui/index.html";
  }

  @GetMapping(
      value = "/api-spec",
      produces = {"application/json", "application/yaml", "application/x-yaml"}
  )
  public ResponseEntity<String> getApiSpec(
      HttpServletRequest request,
      @RequestHeader(name = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
      @RequestParam(required = false) String schemaId) {

    try {
      var schemaInfo = (schemaId != null)
          ? schemaManager.getSchema(schemaId).orElse(null)
          : schemaManager.getActiveSchema().orElse(null);

      if (schemaId != null && schemaInfo == null) {
        return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
            .body("{\"error\":\"Schema not found\"}");
      }

      String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
      String serverUrl = joinPath(base, "/mock");

      boolean wantYaml = accept.toLowerCase(Locale.ROOT).contains("yaml") || accept.toLowerCase(Locale.ROOT).contains("yml");
      var writer = wantYaml ? yamlMapper : objectMapper;
      var contentType = wantYaml
          ? MediaType.parseMediaType("application/yaml")
          : MediaType.APPLICATION_JSON;

      if (schemaInfo == null) {
        String body = writer.writeValueAsString(defaultSpec(serverUrl,
            "No Schema Loaded",
            "Please upload an OpenAPI specification"));
        return withCaching(body, contentType, request);
      }

      var openApiIndex = schemaInfo.getIndex();

      if (openApiIndex.getRawSpecContent() != null) {
        String rawContent = openApiIndex.getRawSpecContent();
        Map<String, Object> spec;

        boolean isYaml = looksLikeYaml(rawContent);
        spec = isYaml
            ? yamlMapper.readValue(rawContent, MAP_TYPE)
            : objectMapper.readValue(rawContent, MAP_TYPE);

        List<Map<String, Object>> servers = getServersList(spec);
        Map<String, Object> localServer = Map.of(
            "url", serverUrl,
            "description", "Mock Server"
        );
        servers.add(0, localServer);
        spec.put("servers", servers);

        String body = writer.writeValueAsString(spec);
        return withCaching(body, contentType, request);
      }

      if (openApiIndex.getOpenAPI() == null) {
        String body = writer.writeValueAsString(defaultSpec(serverUrl,
            "Smart Mock Server",
            "No OpenAPI specification loaded. Please upload a spec file."));
        return withCaching(body, contentType, request);
      }

      OpenAPI spec = deepClone(openApiIndex.getOpenAPI());
      Server server = new Server().url(serverUrl).description("Mock Server");
      List<Server> servers = new ArrayList<>();
      servers.add(server);
      if (spec.getServers() != null) {
        servers.addAll(spec.getServers());
      }
      spec.setServers(servers);

      Json.mapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
      String bodyJson = io.swagger.v3.core.util.Json.pretty(spec);
      String body = wantYaml ? yamlMapper.writeValueAsString(objectMapper.readValue(bodyJson, MAP_TYPE)) : bodyJson;

      return withCaching(body, contentType, request);

    } catch (Exception e) {
      log.error("Failed to render API spec", e);
      return ResponseEntity.internalServerError()
          .contentType(MediaType.APPLICATION_JSON)
          .body("{\"error\":\"Failed to render API spec\"}");
    }
  }

  private static boolean looksLikeYaml(String s) {
    String t = s.trim();
    return t.startsWith("openapi:") || t.startsWith("swagger:");
  }

  private static List<Map<String, Object>> getServersList(Map<String, Object> spec) {
    Object current = spec.get("servers");
    if (current instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
      return new ArrayList<>((List<Map<String, Object>>) current);
    }
    return List.of();
  }

  private static Map<String, Object> defaultSpec(String serverUrl, String title, String description) {
    return Map.of(
        "openapi", "3.0.0",
        "info", Map.of(
            "title", title,
            "version", "1.0.0",
            "description", description
        ),
        "servers", List.of(Map.of(
            "url", serverUrl,
            "description", "Mock Server"
        )),
        "paths", Map.of()
    );
  }


  private static String joinPath(String base, String suffix) {
    if (!StringUtils.hasText(base)) return suffix;
    if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
    return base + suffix;
  }

  private static OpenAPI deepClone(OpenAPI original) throws Exception {
    byte[] bytes = Json.mapper().writeValueAsBytes(original);
    return Json.mapper().readValue(bytes, OpenAPI.class);
  }

  private static ResponseEntity<String> withCaching(String body, MediaType contentType, HttpServletRequest request) throws Exception {
    String etag = quote(etagFor(body));
    String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
    if (etag.equals(ifNoneMatch)) {
      return ResponseEntity.status(304).eTag(etag).build();
    }
    return ResponseEntity.ok()
        .eTag(etag)
        .contentType(contentType)
        .cacheControl(org.springframework.http.CacheControl.noCache())
        .body(body);
  }

  private static String etagFor(String body) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(body.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
  }

  private static String quote(String s) {
    if (s == null) return "\"\"";
    if (s.startsWith("\"") && s.endsWith("\"")) return s;
    return "\"" + s + "\"";
  }

}