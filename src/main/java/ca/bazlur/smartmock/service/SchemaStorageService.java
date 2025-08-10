package ca.bazlur.smartmock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple storage service that persists schemas to disk
 * without creating circular dependencies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaStorageService {
    
    private final ObjectMapper objectMapper;
    
    @Value("${smart-mock.storage.path:${user.home}/.smart-mock/schemas}")
    private String storagePath;
    
    @Value("${smart-mock.storage.enabled:true}")
    private boolean storageEnabled;
    
    @Data
    public static class StoredSchema {
        private String id;
        private String name;
        private String description;
        private String specContent;
        private LocalDateTime uploadedAt;
        private boolean isActive;
    }
    
    @PostConstruct
    public void init() {
        if (!storageEnabled) {
            log.info("Schema storage is disabled");
            return;
        }
        
        try {
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                log.info("Created storage directory: {}", storageDir);
            }
        } catch (IOException e) {
            log.error("Failed to initialize storage directory: {}", e.getMessage());
        }
    }
    
    /**
     * Save a schema to disk
     */
    public void saveSchema(String id, String name, String description, String specContent, 
                          LocalDateTime uploadedAt, boolean isActive) {
        if (!storageEnabled) {
            return;
        }
        
        try {
            Path storageDir = Paths.get(storagePath);
            Path schemaFile = storageDir.resolve(id + ".schema.json");
            
            StoredSchema stored = new StoredSchema();
            stored.setId(id);
            stored.setName(name);
            stored.setDescription(description);
            stored.setSpecContent(specContent);
            stored.setUploadedAt(uploadedAt);
            stored.setActive(isActive);
            
            objectMapper.writeValue(schemaFile.toFile(), stored);
            log.debug("Saved schema '{}' to {}", name, schemaFile);
            
        } catch (IOException e) {
            log.error("Failed to save schema '{}': {}", id, e.getMessage());
        }
    }
    
    /**
     * Load all schemas from disk
     */
    public List<StoredSchema> loadAllSchemas() {
        List<StoredSchema> schemas = new ArrayList<>();
        
        if (!storageEnabled) {
            return schemas;
        }
        
        try {
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                return schemas;
            }
            
            List<Path> schemaFiles = Files.list(storageDir)
                .filter(p -> p.toString().endsWith(".schema.json"))
                .sorted()
                .collect(Collectors.toList());
            
            for (Path schemaFile : schemaFiles) {
                try {
                    StoredSchema stored = objectMapper.readValue(
                        schemaFile.toFile(), StoredSchema.class);
                    schemas.add(stored);
                    log.debug("Loaded schema '{}' from {}", stored.getName(), schemaFile);
                } catch (Exception e) {
                    log.error("Failed to load schema from {}: {}", schemaFile, e.getMessage());
                }
            }
            
            log.info("Loaded {} schemas from disk", schemas.size());
            
        } catch (IOException e) {
            log.error("Failed to load schemas: {}", e.getMessage());
        }
        
        return schemas;
    }
    
    /**
     * Delete a schema file from disk
     */
    public void deleteSchema(String schemaId) {
        if (!storageEnabled) {
            return;
        }
        
        try {
            Path storageDir = Paths.get(storagePath);
            Path schemaFile = storageDir.resolve(schemaId + ".schema.json");
            
            if (Files.exists(schemaFile)) {
                Files.delete(schemaFile);
                log.debug("Deleted schema file: {}", schemaFile);
            }
            
        } catch (IOException e) {
            log.error("Failed to delete schema {}: {}", schemaId, e.getMessage());
        }
    }
    
    /**
     * Clear all stored schemas
     */
    public void clearAllSchemas() {
        if (!storageEnabled) {
            return;
        }
        
        try {
            Path storageDir = Paths.get(storagePath);
            if (Files.exists(storageDir)) {
                Files.list(storageDir)
                    .filter(p -> p.toString().endsWith(".schema.json"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            log.debug("Deleted schema file: {}", p);
                        } catch (IOException e) {
                            log.warn("Failed to delete schema file: {}", p);
                        }
                    });
            }
            log.info("Cleared all stored schemas");
            
        } catch (IOException e) {
            log.error("Failed to clear schemas: {}", e.getMessage());
        }
    }
}