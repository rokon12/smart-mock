package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.openapi.OpenApiIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ApiExplorerController {
    
    private final OpenApiIndex openApiIndex;
    private final ObjectMapper objectMapper;
    
    @GetMapping("/explorer")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui/index.html";
    }
    
    @GetMapping(value = "/api-spec", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> getApiSpec() {
        try {
            // First try to return the raw spec content if available
            if (openApiIndex.getRawSpecContent() != null) {
                // Convert YAML to JSON if needed
                String rawContent = openApiIndex.getRawSpecContent();
                if (rawContent.trim().startsWith("openapi:") || rawContent.trim().startsWith("swagger:")) {
                    // It's YAML, need to convert to JSON using Jackson's YAML support
                    com.fasterxml.jackson.dataformat.yaml.YAMLMapper yamlMapper = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper();
                    Object yamlObj = yamlMapper.readValue(rawContent, Object.class);
                    return ResponseEntity.ok(objectMapper.writeValueAsString(yamlObj));
                } else {
                    // Already JSON
                    return ResponseEntity.ok(rawContent);
                }
            }
            
            // Fallback: Return a default spec when nothing is loaded
            if (openApiIndex.getOpenAPI() == null) {
                String defaultSpec = """
                    {
                      "openapi": "3.0.0",
                      "info": {
                        "title": "Smart Mock Server",
                        "version": "1.0",
                        "description": "No OpenAPI specification loaded. Please upload a spec file."
                      },
                      "paths": {}
                    }
                    """;
                return ResponseEntity.ok(defaultSpec);
            }
            
            // If we have a parsed spec but no raw content, try to serialize it cleanly
            // This is a fallback and may have issues with complex schemas
            io.swagger.v3.core.util.Json.mapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            String json = io.swagger.v3.core.util.Json.pretty(openApiIndex.getOpenAPI());
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    @GetMapping(value = "/swagger-ui/swagger-initializer.js", produces = "application/javascript")
    @ResponseBody
    public String swaggerInitializer() {
        return """
            window.onload = function() {
              window.ui = SwaggerUIBundle({
                url: "/api-spec",
                dom_id: '#swagger-ui',
                deepLinking: true,
                presets: [
                  SwaggerUIBundle.presets.apis,
                  SwaggerUIStandalonePreset
                ],
                plugins: [
                  SwaggerUIBundle.plugins.DownloadUrl
                ],
                layout: "StandaloneLayout",
                tryItOutEnabled: true,
                filter: true,
                displayRequestDuration: true,
                docExpansion: "none",
                operationsSorter: "method",
                tagsSorter: "alpha"
              });
            };
            """;
    }
}