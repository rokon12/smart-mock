package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.openapi.OpenApiIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public ResponseEntity<String> getApiSpec(HttpServletRequest request) {
        try {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String contextPath = request.getContextPath();
            
            String serverUrl = scheme + "://" + serverName;
            if ((scheme.equals("http") && serverPort != 80) || 
                (scheme.equals("https") && serverPort != 443)) {
                serverUrl += ":" + serverPort;
            }
            serverUrl += contextPath + "/mock";
            
            if (openApiIndex.getRawSpecContent() != null) {
                String rawContent = openApiIndex.getRawSpecContent();
                Map<String, Object> spec;
                
                if (rawContent.trim().startsWith("openapi:") || rawContent.trim().startsWith("swagger:")) {
                    var yamlMapper = new YAMLMapper();
                    spec = yamlMapper.readValue(rawContent, Map.class);
                } else {
                    spec = objectMapper.readValue(rawContent, Map.class);
                }
                
                List<Map<String, Object>> servers = new ArrayList<>();
                Map<String, Object> localServer = new HashMap<>();
                localServer.put("url", serverUrl);
                localServer.put("description", "Mock Server");
                servers.add(localServer);
                spec.put("servers", servers);
                
                return ResponseEntity.ok(objectMapper.writeValueAsString(spec));
            }
            
            if (openApiIndex.getOpenAPI() == null) {
                Map<String, Object> defaultSpec = new HashMap<>();
                defaultSpec.put("openapi", "3.0.0");
                
                Map<String, Object> info = new HashMap<>();
                info.put("title", "Smart Mock Server");
                info.put("version", "1.0");
                info.put("description", "No OpenAPI specification loaded. Please upload a spec file.");
                defaultSpec.put("info", info);
                
                List<Map<String, Object>> servers = new ArrayList<>();
                Map<String, Object> localServer = new HashMap<>();
                localServer.put("url", serverUrl);
                localServer.put("description", "Mock Server");
                servers.add(localServer);
                defaultSpec.put("servers", servers);
                
                defaultSpec.put("paths", new HashMap<>());
                
                return ResponseEntity.ok(objectMapper.writeValueAsString(defaultSpec));
            }
            
            OpenAPI spec = openApiIndex.getOpenAPI();
            
            Server server = new Server();
            server.setUrl(serverUrl);
            server.setDescription("Mock Server");
            spec.setServers(List.of(server));
            
            Json.mapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
          return ResponseEntity.ok(Json.pretty(spec));
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