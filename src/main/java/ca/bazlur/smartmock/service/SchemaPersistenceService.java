package ca.bazlur.smartmock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class SchemaPersistenceService {
    
    private final SchemaManager schemaManager;
    private final SchemaStorageService storageService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        loadSchemas();
    }
    
    @PreDestroy
    public void shutdown() {
        saveAllSchemas();
    }
    
    @Scheduled(fixedDelayString = "300000", initialDelayString = "60000")
    public void periodicSave() {
        saveAllSchemas();
    }
    
    public void saveAllSchemas() {
        try {
            storageService.clearAllSchemas();
            
            for (SchemaManager.SchemaInfo schema : schemaManager.getAllSchemas()) {
                boolean isActive = schemaManager.getActiveSchema()
                    .map(s -> s.getId().equals(schema.getId()))
                    .orElse(false);
                
                storageService.saveSchema(
                    schema.getId(),
                    schema.getName(),
                    schema.getDescription(),
                    schema.getIndex().getRawSpecContent(),
                    schema.getUploadedAt(),
                    isActive
                );
            }
            
            log.info("Saved {} schemas to disk", schemaManager.getAllSchemas().size());
            
        } catch (Exception e) {
            log.error("Failed to save schemas: {}", e.getMessage());
        }
    }
    
    public void loadSchemas() {
        try {
            var storedSchemas = storageService.loadAllSchemas();
            
            if (storedSchemas.isEmpty()) {
                log.info("No persisted schemas found");
                return;
            }
            
            String activeSchemaId = null;
            int loadedCount = 0;
            
            for (var stored : storedSchemas) {
                try {
                    String newId = schemaManager.addSchema(
                        stored.getSpecContent(), 
                        stored.getName()
                    );
                    
                    if (stored.isActive()) {
                        activeSchemaId = newId;
                    }
                    
                    loadedCount++;
                    log.debug("Loaded schema '{}' from storage", stored.getName());
                    
                } catch (Exception e) {
                    log.error("Failed to load schema '{}': {}", stored.getName(), e.getMessage());
                }
            }
            
            if (activeSchemaId != null) {
                schemaManager.setActiveSchema(activeSchemaId);
                log.info("Restored active schema: {}", activeSchemaId);
            }
            
            log.info("Loaded {} schemas from disk", loadedCount);
            
        } catch (Exception e) {
            log.error("Failed to load schemas: {}", e.getMessage());
        }
    }
    
    public void deletePersistedSchema(String schemaId) {
        storageService.deleteSchema(schemaId);
        saveAllSchemas();
    }
    
    public void exportSchema(String schemaId, Path targetPath) throws IOException {
        SchemaManager.SchemaInfo schema = schemaManager.getSchema(schemaId)
            .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaId));
        
        String content = schema.getIndex().getRawSpecContent();
        java.nio.file.Files.writeString(targetPath, content);
        log.info("Exported schema '{}' to {}", schema.getName(), targetPath);
    }
    
    public int importSchemas(Path sourceDir) throws IOException {
        if (!java.nio.file.Files.exists(sourceDir) || !java.nio.file.Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("Invalid source directory: " + sourceDir);
        }
        
        var specFiles = java.nio.file.Files.list(sourceDir)
            .filter(p -> {
                String name = p.toString().toLowerCase();
                return name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json");
            })
            .toList();
        
        int imported = 0;
        for (Path specFile : specFiles) {
            try {
                String content = java.nio.file.Files.readString(specFile);
                String name = specFile.getFileName().toString()
                    .replaceAll("\\.(yaml|yml|json)$", "");
                
                schemaManager.addSchema(content, name);
                imported++;
                log.info("Imported schema from {}", specFile);
                
            } catch (Exception e) {
                log.error("Failed to import {}: {}", specFile, e.getMessage());
            }
        }
        
        if (imported > 0) {
            saveAllSchemas();
        }
        
        return imported;
    }
}