package ca.bazlur.smartmock.service;

import ca.bazlur.smartmock.openapi.OpenApiIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaPersistenceServiceTest {

    @Mock
    private SchemaManager schemaManager;

    @Mock
    private SchemaStorageService storageService;
    
    @Mock
    private OpenApiIndex openApiIndex;

    @InjectMocks
    private SchemaPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new SchemaPersistenceService(schemaManager, storageService);
    }

    @Test
    void onApplicationReady_givenValidSetup_shouldLoadSchemas() {
        var storedSchema = new SchemaStorageService.StoredSchema();
        storedSchema.setId("schema1");
        storedSchema.setName("Test Schema");
        storedSchema.setSpecContent("content");
        storedSchema.setActive(false);
        storedSchema.setUploadedAt(LocalDateTime.now());
        
        when(storageService.loadAllSchemas()).thenReturn(List.of(storedSchema));
        when(schemaManager.addSchema(eq("content"), eq("Test Schema"))).thenReturn("new-schema-id");

        service.onApplicationReady();

        verify(storageService).loadAllSchemas();
        verify(schemaManager).addSchema(eq("content"), eq("Test Schema"));
    }

    @Test
    void onApplicationReady_givenActiveSchema_shouldSetActive() {
        var storedSchema = new SchemaStorageService.StoredSchema();
        storedSchema.setId("schema1");
        storedSchema.setName("Test Schema");
        storedSchema.setSpecContent("content");
        storedSchema.setActive(true);
        storedSchema.setUploadedAt(LocalDateTime.now());
        
        when(storageService.loadAllSchemas()).thenReturn(List.of(storedSchema));
        when(schemaManager.addSchema(eq("content"), eq("Test Schema"))).thenReturn("new-schema-id");

        service.onApplicationReady();

        verify(schemaManager).addSchema(eq("content"), eq("Test Schema"));
        verify(schemaManager).setActiveSchema(eq("new-schema-id"));
    }

    @Test
    void shutdown_givenSchemasPresent_shouldSaveAll() {
        service.shutdown();

        verify(storageService).clearAllSchemas();
        verify(schemaManager, times(2)).getAllSchemas();
    }

    @Test
    void periodicSave_givenSchemasPresent_shouldSaveAll() {
        var schemaInfo = new SchemaManager.SchemaInfo("schema1", "Test Schema", "description", openApiIndex);
        when(schemaManager.getAllSchemas()).thenReturn(List.of(schemaInfo));
        when(schemaManager.getActiveSchema()).thenReturn(Optional.of(schemaInfo));

        service.periodicSave();

        verify(storageService).clearAllSchemas();
        verify(storageService).saveSchema(
            eq("schema1"), 
            eq("Test Schema"), 
            eq("description"),
            any(),
            any(LocalDateTime.class),
            eq(true)
        );
        verify(schemaManager, times(2)).getAllSchemas();
    }

    @Test
    void saveAllSchemas_givenMultipleSchemas_shouldSaveAll() {
        var schema1 = new SchemaManager.SchemaInfo("schema1", "Test Schema 1", "desc1", openApiIndex);
        var schema2 = new SchemaManager.SchemaInfo("schema2", "Test Schema 2", "desc2", openApiIndex);
        when(schemaManager.getAllSchemas()).thenReturn(List.of(schema1, schema2));
        when(schemaManager.getActiveSchema()).thenReturn(Optional.of(schema1));

        service.saveAllSchemas();

        verify(storageService).clearAllSchemas();
        verify(storageService).saveSchema(
            eq("schema1"), 
            eq("Test Schema 1"), 
            eq("desc1"),
            any(),
            any(LocalDateTime.class),
            eq(true)
        );
        verify(storageService).saveSchema(
            eq("schema2"), 
            eq("Test Schema 2"), 
            eq("desc2"),
            any(),
            any(LocalDateTime.class),
            eq(false)
        );
    }

    @Test
    void saveAllSchemas_givenNoActiveSchema_shouldSaveAllAsInactive() {
        var schema1 = new SchemaManager.SchemaInfo("schema1", "Test Schema 1", "desc", openApiIndex);
        when(schemaManager.getAllSchemas()).thenReturn(List.of(schema1));
        when(schemaManager.getActiveSchema()).thenReturn(Optional.empty());

        service.saveAllSchemas();

        verify(storageService).clearAllSchemas();
        verify(storageService).saveSchema(
            eq("schema1"), 
            eq("Test Schema 1"), 
            eq("desc"),
            any(),
            any(LocalDateTime.class),
            eq(false)
        );
    }

    @Test
    void saveAllSchemas_givenSaveFailure_shouldLogError() {
        var schema1 = new SchemaManager.SchemaInfo("schema1", "Test Schema", "desc", openApiIndex);
        when(schemaManager.getAllSchemas()).thenReturn(List.of(schema1));
        when(schemaManager.getActiveSchema()).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Save failed"))
            .when(storageService).saveSchema(anyString(), anyString(), anyString(), any(), any(LocalDateTime.class), anyBoolean());

        service.saveAllSchemas();

        verify(storageService).clearAllSchemas();
        verify(storageService).saveSchema(
            eq("schema1"), 
            eq("Test Schema"), 
            eq("desc"),
            any(),
            any(LocalDateTime.class),
            eq(false)
        );
    }

    @Test
    void loadSchemas_givenEmptyStorage_shouldNotAddAnySchemas() {
        when(storageService.loadAllSchemas()).thenReturn(List.of());

        service.loadSchemas();

        verify(storageService).loadAllSchemas();
        verify(schemaManager, never()).addSchema(anyString(), anyString());
        verify(schemaManager, never()).setActiveSchema(anyString());
    }

    @Test
    void loadSchemas_givenLoadFailure_shouldHandleGracefully() {
        when(storageService.loadAllSchemas()).thenThrow(new RuntimeException("Load failed"));

        service.loadSchemas();

        verify(storageService).loadAllSchemas();
        verify(schemaManager, never()).addSchema(anyString(), anyString());
    }
}