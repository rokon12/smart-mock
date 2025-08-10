package ca.bazlur.smartmock.service;

import ca.bazlur.smartmock.openapi.OpenApiIndex;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SchemaManager {
    
    @Getter
    public static class SchemaInfo {
        private final String id;
        private final String name;
        private final String description;
        private final OpenApiIndex index;
        private final LocalDateTime uploadedAt;
        private final int endpointCount;
        private final String version;
        
        public SchemaInfo(String id, String name, String description, OpenApiIndex index) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.index = index;
            this.uploadedAt = LocalDateTime.now();
            
            // Extract metadata from OpenAPI
            if (index.getOpenAPI() != null) {
                OpenAPI api = index.getOpenAPI();
                this.version = api.getInfo() != null ? api.getInfo().getVersion() : "1.0.0";
                this.endpointCount = api.getPaths() != null ? api.getPaths().size() : 0;
            } else {
                this.version = "1.0.0";
                this.endpointCount = 0;
            }
        }
    }
    
    private final Map<String, SchemaInfo> schemas = new ConcurrentHashMap<>();
    private volatile String activeSchemaId;
    
    /**
     * Add a new schema to the manager
     */
    public String addSchema(String specContent, String name) {
        String id = generateSchemaId(name);
        
        OpenApiIndex index = new OpenApiIndex();
        index.loadSpec(specContent);
        
        if (index.getOpenAPI() == null) {
            throw new IllegalArgumentException("Invalid OpenAPI specification");
        }
        
        // Extract name and description from spec if not provided
        if (name == null && index.getOpenAPI().getInfo() != null) {
            name = index.getOpenAPI().getInfo().getTitle();
        }
        if (name == null) {
            name = "Schema " + (schemas.size() + 1);
        }
        
        String description = null;
        if (index.getOpenAPI().getInfo() != null) {
            description = index.getOpenAPI().getInfo().getDescription();
        }
        
        SchemaInfo schemaInfo = new SchemaInfo(id, name, description, index);
        schemas.put(id, schemaInfo);
        
        // If this is the first schema or no active schema, make it active
        if (activeSchemaId == null || schemas.size() == 1) {
            activeSchemaId = id;
        }
        
        log.info("Added schema '{}' with ID '{}'. Total schemas: {}", name, id, schemas.size());
        return id;
    }
    
    /**
     * Get all schemas
     */
    public Collection<SchemaInfo> getAllSchemas() {
        return new ArrayList<>(schemas.values());
    }
    
    /**
     * Get a specific schema
     */
    public Optional<SchemaInfo> getSchema(String id) {
        return Optional.ofNullable(schemas.get(id));
    }
    
    /**
     * Get the active schema
     */
    public Optional<SchemaInfo> getActiveSchema() {
        if (activeSchemaId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(schemas.get(activeSchemaId));
    }
    
    /**
     * Set the active schema
     */
    public boolean setActiveSchema(String id) {
        if (!schemas.containsKey(id)) {
            log.warn("Attempted to set non-existent schema '{}' as active", id);
            return false;
        }
        
        activeSchemaId = id;
        log.info("Set active schema to '{}'", id);
        return true;
    }
    
    /**
     * Delete a schema
     */
    public boolean deleteSchema(String id) {
        SchemaInfo removed = schemas.remove(id);
        if (removed != null) {
            // If we deleted the active schema, pick another one
            if (id.equals(activeSchemaId)) {
                if (!schemas.isEmpty()) {
                    activeSchemaId = schemas.keySet().iterator().next();
                    log.info("Active schema deleted. New active schema: '{}'", activeSchemaId);
                } else {
                    activeSchemaId = null;
                    log.info("Active schema deleted. No schemas remaining.");
                }
            }
            log.info("Deleted schema '{}'", id);
            return true;
        }
        return false;
    }
    
    /**
     * Clear all schemas
     */
    public void clearAll() {
        schemas.clear();
        activeSchemaId = null;
        log.info("Cleared all schemas");
    }
    
    /**
     * Get the active OpenAPI index for mock operations
     */
    public Optional<OpenApiIndex> getActiveIndex() {
        return getActiveSchema().map(SchemaInfo::getIndex);
    }
    
    /**
     * Check if a schema name already exists
     */
    public boolean schemaNameExists(String name) {
        return schemas.values().stream()
            .anyMatch(s -> s.getName().equalsIgnoreCase(name));
    }
    
    /**
     * Update a schema
     */
    public boolean updateSchema(String id, String specContent) {
        SchemaInfo existing = schemas.get(id);
        if (existing == null) {
            return false;
        }
        
        OpenApiIndex newIndex = new OpenApiIndex();
        newIndex.loadSpec(specContent);
        
        if (newIndex.getOpenAPI() == null) {
            throw new IllegalArgumentException("Invalid OpenAPI specification");
        }
        
        // Create updated schema info preserving the name
        SchemaInfo updated = new SchemaInfo(id, existing.getName(), 
            newIndex.getOpenAPI().getInfo() != null ? newIndex.getOpenAPI().getInfo().getDescription() : null,
            newIndex);
        
        schemas.put(id, updated);
        log.info("Updated schema '{}'", id);
        return true;
    }
    
    private String generateSchemaId(String name) {
        String base = name != null ? 
            name.toLowerCase().replaceAll("[^a-z0-9]", "-") : 
            "schema";
        
        String id = base;
        int counter = 1;
        while (schemas.containsKey(id)) {
            id = base + "-" + counter++;
        }
        return id;
    }
}