package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.service.SchemaManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/schemas")
@RequiredArgsConstructor
public class SchemaController {
    
    private final SchemaManager schemaManager;
    
    @GetMapping
    public ResponseEntity<Collection<SchemaManager.SchemaInfo>> listSchemas() {
        return ResponseEntity.ok(schemaManager.getAllSchemas());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SchemaManager.SchemaInfo> getSchema(@PathVariable String id) {
        return schemaManager.getSchema(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/active")
    public ResponseEntity<SchemaManager.SchemaInfo> getActiveSchema() {
        return schemaManager.getActiveSchema()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Map<String, String>> uploadSchema(
            @RequestBody String specContent,
            @RequestParam(required = false) String name) {
        try {
            String id = schemaManager.addSchema(specContent, name);
            Map<String, String> response = new HashMap<>();
            response.put("id", id);
            response.put("message", "Schema uploaded successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Failed to upload schema", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateSchema(
            @PathVariable String id,
            @RequestBody String specContent) {
        try {
            if (schemaManager.updateSchema(id, specContent)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Schema updated successfully");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Failed to update schema", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSchema(@PathVariable String id) {
        if (schemaManager.deleteSchema(id)) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Schema deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/activate")
    public ResponseEntity<Map<String, String>> activateSchema(@PathVariable String id) {
        if (schemaManager.setActiveSchema(id)) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Schema activated successfully");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/load-samples")
    public ResponseEntity<Map<String, Object>> loadSampleSchemas() {
        Map<String, Object> response = new HashMap<>();
        int loaded = 0;
        
        try {
            // Load Pet Store sample
            ClassPathResource petStore = new ClassPathResource("sample-petstore.yaml");
            String petStoreContent = new String(petStore.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            schemaManager.addSchema(petStoreContent, "Pet Store API");
            loaded++;
            
            // Load E-commerce sample if it exists
            try {
                ClassPathResource ecommerce = new ClassPathResource("ecommerce-api.yaml");
                String ecommerceContent = new String(ecommerce.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                schemaManager.addSchema(ecommerceContent, "E-Commerce API");
                loaded++;
            } catch (Exception e) {
                log.debug("E-commerce sample not found, skipping");
            }
            
            response.put("loaded", loaded);
            response.put("message", "Sample schemas loaded successfully");
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Failed to load sample schemas", e);
            response.put("error", "Failed to load sample schemas: " + e.getMessage());
            response.put("loaded", loaded);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearAllSchemas() {
        schemaManager.clearAll();
        Map<String, String> response = new HashMap<>();
        response.put("message", "All schemas cleared successfully");
        return ResponseEntity.ok(response);
    }
}