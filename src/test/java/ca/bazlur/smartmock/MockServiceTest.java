package ca.bazlur.smartmock;

import ca.bazlur.smartmock.service.SchemaManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class MockServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchemaManager schemaManager;

    @BeforeEach
    void setUp() throws Exception {
        ClassPathResource resource = new ClassPathResource("sample-petstore.yaml");
        String spec = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        String schemaId = schemaManager.addSchema(spec, "Petstore API");
        schemaManager.setActiveSchema(schemaId);
    }

    @Test
    void testListPets_HappyPath() throws Exception {
        mockMvc.perform(get("/mock/pets?limit=5")
                .header("X-Mock-Scenario", "happy"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void testGetPetById_NotFound() throws Exception {
        mockMvc.perform(get("/mock/pets/999")
                .header("X-Mock-Scenario", "invalid"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testCreatePet() throws Exception {
        String petInput = """
                {
                    "name": "Fluffy",
                    "category": "cat",
                    "status": "available",
                    "tags": ["cute", "playful"]
                }
                """;

        mockMvc.perform(post("/mock/pets")
                .header("Content-Type", "application/json")
                .header("X-Mock-Scenario", "happy")
                .content(petInput))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void testServerError() throws Exception {
        mockMvc.perform(get("/mock/pets")
                .header("X-Mock-Scenario", "server-error"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testRateLimitScenario() throws Exception {
        mockMvc.perform(get("/mock/pets")
                .header("X-Mock-Scenario", "rate-limit"))
                .andExpect(status().is(429));
    }

    @Test
    void testWithSeed() throws Exception {
        String seed = "test-seed-123";
        
        mockMvc.perform(get("/mock/pets/1")
                .header("X-Mock-Seed", seed))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void testPaginatedUsers() throws Exception {
        mockMvc.perform(get("/mock/users?page=2&size=10")
                .header("X-Mock-Scenario", "happy"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }
}