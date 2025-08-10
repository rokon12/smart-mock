package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.openapi.OpenApiIndex;
import ca.bazlur.smartmock.service.SchemaManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final OpenApiIndex openApiIndex;
    private final SchemaManager schemaManager;

    @PostMapping("/spec")
    public ResponseEntity<String> uploadSpec(@RequestBody String specContent) {
        // For backward compatibility, also load into the legacy OpenApiIndex
        openApiIndex.loadSpec(specContent);
        
        // Add to the schema manager as well
        try {
            schemaManager.addSchema(specContent, null);
        } catch (Exception e) {
            log.error("Failed to add schema to manager", e);
        }
        
        return ResponseEntity.ok("OpenAPI spec loaded successfully");
    }

    @PostMapping("/spec/file")
    public ResponseEntity<String> uploadSpecFile(@RequestParam String filePath) {
        if (filePath.contains("sample-petstore.yaml")) {
            return loadSampleSpec();
        }
        
        try {
            openApiIndex.loadSpecFromFile(filePath);
            return ResponseEntity.ok("OpenAPI spec loaded from file successfully");
        } catch (Exception e) {
            log.warn("Failed to load from file path, trying as classpath resource: {}", filePath);
            try {
                ClassPathResource resource = new ClassPathResource(filePath);
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                openApiIndex.loadSpec(content);
                return ResponseEntity.ok("OpenAPI spec loaded from classpath successfully");
            } catch (IOException ioException) {
                log.error("Failed to load spec from file or classpath: {}", filePath, ioException);
                return ResponseEntity.internalServerError().body("Failed to load spec: " + ioException.getMessage());
            }
        }
    }
    
    @PostMapping("/spec/sample")
    public ResponseEntity<String> loadSampleSpec() {
        try {
            ClassPathResource resource = new ClassPathResource("sample-petstore.yaml");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            openApiIndex.loadSpec(content);
            
            // Also add to schema manager
            try {
                schemaManager.addSchema(content, "Pet Store API");
            } catch (Exception e) {
                log.error("Failed to add sample to schema manager", e);
            }
            
            return ResponseEntity.ok("Sample Pet Store spec loaded successfully");
        } catch (IOException e) {
            log.error("Failed to load sample spec from classpath", e);
            return ResponseEntity.internalServerError().body("Failed to load sample spec: " + e.getMessage());
        }
    }

    @GetMapping("/spec")
    public ResponseEntity<Object> getSpec() {
        if (openApiIndex.getOpenAPI() != null) {
            return ResponseEntity.ok(openApiIndex.getOpenAPI());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/spec")
    public ResponseEntity<String> clearSpec() {
        openApiIndex.clear();
        schemaManager.clearAll();
        return ResponseEntity.ok("OpenAPI spec cleared successfully");
    }
}