package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.service.SchemaManager;
import ca.bazlur.smartmock.service.SchemaPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/schemas")
@RequiredArgsConstructor
public class SchemaController {
    
    private final SchemaManager schemaManager;
    private final SchemaPersistenceService persistenceService;
    
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
    
    @GetMapping("/{id}/export")
    public ResponseEntity<Resource> exportSchema(@PathVariable String id) {
        try {
            SchemaManager.SchemaInfo schema = schemaManager.getSchema(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schema not found"));
            
            String content = schema.getIndex().getRawSpecContent();
            ByteArrayResource resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
            
            String filename = schema.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".yaml";
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(content.length())
                .body(resource);
                
        } catch (Exception e) {
            log.error("Failed to export schema", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to export schema");
        }
    }
    
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importSchemas(@RequestParam("files") MultipartFile[] files) {
        Map<String, Object> response = new HashMap<>();
        int imported = 0;
        int failed = 0;
        
        for (MultipartFile file : files) {
            try {
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                String name = file.getOriginalFilename()
                    .replaceAll("\\.(yaml|yml|json)$", "");
                
                schemaManager.addSchema(content, name);
                imported++;
                log.info("Imported schema from file: {}", file.getOriginalFilename());
                
            } catch (Exception e) {
                log.error("Failed to import file: {}", file.getOriginalFilename(), e);
                failed++;
            }
        }
        
        response.put("imported", imported);
        response.put("failed", failed);
        response.put("message", String.format("Imported %d schemas, %d failed", imported, failed));
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/backup")
    public ResponseEntity<Map<String, String>> backupSchemas() {
        persistenceService.saveAllSchemas();
        Map<String, String> response = new HashMap<>();
        response.put("message", "All schemas backed up successfully");
        response.put("count", String.valueOf(schemaManager.getAllSchemas().size()));
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/restore")
    public ResponseEntity<Map<String, String>> restoreSchemas() {
        persistenceService.loadSchemas();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Schemas restored from backup");
        response.put("count", String.valueOf(schemaManager.getAllSchemas().size()));
        return ResponseEntity.ok(response);
    }
}