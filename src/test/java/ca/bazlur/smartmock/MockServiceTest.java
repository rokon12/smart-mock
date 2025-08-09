package ca.bazlur.smartmock;

import ca.bazlur.smartmock.openapi.OpenApiIndex;
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
    private OpenApiIndex openApiIndex;

    @BeforeEach
    void setUp() throws Exception {
        ClassPathResource resource = new ClassPathResource("sample-petstore.yaml");
        String spec = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        openApiIndex.loadSpec(spec);
    }

    @Test
    void testListPets_HappyPath() throws Exception {
        mockMvc.perform(get("/mock/pets?limit=5")
                .header("X-Mock-Scenario", "happy"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andExpect(jsonPath("$[0].status", is(oneOf("available", "pending", "sold"))));
    }

    @Test
    void testGetPetById_NotFound() throws Exception {
        mockMvc.perform(get("/mock/pets/999")
                .header("X-Mock-Scenario", "invalid"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isNotEmpty());
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
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Fluffy"))
                .andExpect(jsonPath("$.category").value("cat"));
    }

    @Test
    void testServerError() throws Exception {
        mockMvc.perform(get("/mock/pets")
                .header("X-Mock-Scenario", "server-error"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void testRateLimitScenario() throws Exception {
        mockMvc.perform(get("/mock/pets")
                .header("X-Mock-Scenario", "rate-limit"))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().exists("X-RateLimit-Limit"));
    }

    @Test
    void testWithSeed() throws Exception {
        String seed = "test-seed-123";
        
        mockMvc.perform(get("/mock/pets/1")
                .header("X-Mock-Seed", seed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void testPaginatedUsers() throws Exception {
        mockMvc.perform(get("/mock/users?page=2&size=10")
                .header("X-Mock-Scenario", "happy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.pagination.page").isNumber())
                .andExpect(jsonPath("$.pagination.size").isNumber());
    }
}