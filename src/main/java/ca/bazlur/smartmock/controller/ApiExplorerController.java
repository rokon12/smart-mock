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
                Map<String, Object> spec;
                
                if (rawContent.trim().startsWith("openapi:") || rawContent.trim().startsWith("swagger:")) {
                    // It's YAML, need to convert to JSON using Jackson's YAML support
                    com.fasterxml.jackson.dataformat.yaml.YAMLMapper yamlMapper = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper();
                    spec = yamlMapper.readValue(rawContent, Map.class);
                } else {
                    // Already JSON
                    spec = objectMapper.readValue(rawContent, Map.class);
                }
                
                // Add/update the servers section to point to our mock endpoint
                java.util.List<Map<String, Object>> servers = new java.util.ArrayList<>();
                Map<String, Object> localServer = new java.util.HashMap<>();
                localServer.put("url", "http://localhost:8080/mock");
                localServer.put("description", "Local Mock Server");
                servers.add(localServer);
                spec.put("servers", servers);
                
                return ResponseEntity.ok(objectMapper.writeValueAsString(spec));
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
                      "servers": [
                        {
                          "url": "http://localhost:8080/mock",
                          "description": "Local Mock Server"
                        }
                      ],
                      "paths": {}
                    }
                    """;
                return ResponseEntity.ok(defaultSpec);
            }
            
            // If we have a parsed spec but no raw content, add servers and serialize
            OpenAPI spec = openApiIndex.getOpenAPI();
            
            // Set up servers to point to our mock endpoint
            io.swagger.v3.oas.models.servers.Server server = new io.swagger.v3.oas.models.servers.Server();
            server.setUrl("http://localhost:8080/mock");
            server.setDescription("Local Mock Server");
            spec.setServers(java.util.Arrays.asList(server));
            
            io.swagger.v3.core.util.Json.mapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            String json = io.swagger.v3.core.util.Json.pretty(spec);
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