package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import ca.bazlur.smartmock.llm.external.ExternalBlockDefinition;
import ca.bazlur.smartmock.llm.external.ExternalBlockLoader;
import ca.bazlur.smartmock.llm.external.ExternalContextBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockManagementControllerTest {

    @Mock
    private ExternalBlockLoader externalBlockLoader;

    @Mock
    private List<ContextBlock> builtInBlocks;

    @InjectMocks
    private BlockManagementController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "blocksPath", "/test/path");
    }

    @Test
    void listBlocks_givenBuiltInAndExternalBlocks_shouldReturnBothWithSummary() {
        var builtInBlock = new TestBlock();
        when(builtInBlocks.stream()).thenReturn(Stream.of(builtInBlock));

        var externalDef = new ExternalBlockDefinition();
        externalDef.setId("external.test");
        externalDef.setName("External Test");
        var externalBlock = new ExternalContextBlock(externalDef);
        when(externalBlockLoader.getExternalBlocks()).thenReturn(List.of(externalBlock));

        ResponseEntity<Map<String, Object>> response = controller.listBlocks();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body).containsKeys("builtIn", "external", "summary", "externalPath");
        
        var summary = (Map<String, Object>) body.get("summary");
        assertThat(summary.get("builtInCount")).isEqualTo(1);
        assertThat(summary.get("externalCount")).isEqualTo(1);
        assertThat(summary.get("totalCount")).isEqualTo(2);
    }

    @Test
    void reloadBlocks_givenRequest_shouldReloadAndReturnStatus() {
        when(externalBlockLoader.getExternalBlocks()).thenReturn(List.of(mock(ContextBlock.class)));

        ResponseEntity<Map<String, Object>> response = controller.reloadBlocks();

        verify(externalBlockLoader).reloadBlocks();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("count")).isEqualTo(1);
    }

    sealed interface BlockDetailsTestCase {
        String blockId();
        boolean isBuiltIn();
        boolean exists();
        
        record BuiltInBlockCase(String blockId) implements BlockDetailsTestCase {
            @Override public boolean isBuiltIn() { return true; }
            @Override public boolean exists() { return true; }
        }
        
        record ExternalBlockCase(String blockId) implements BlockDetailsTestCase {
            @Override public boolean isBuiltIn() { return false; }
            @Override public boolean exists() { return true; }
        }
        
        record NotFoundCase(String blockId) implements BlockDetailsTestCase {
            @Override public boolean isBuiltIn() { return false; }
            @Override public boolean exists() { return false; }
        }
    }

    static Stream<BlockDetailsTestCase> blockDetailsTestCases() {
        return Stream.of(
            new BlockDetailsTestCase.BuiltInBlockCase("builtin.test"),
            new BlockDetailsTestCase.ExternalBlockCase("external.test"),
            new BlockDetailsTestCase.NotFoundCase("nonexistent.block")
        );
    }

    @ParameterizedTest
    @MethodSource("blockDetailsTestCases")
    void getBlockDetails_givenVariousBlocks_shouldReturnAppropriateResponse(BlockDetailsTestCase testCase) {
        if (testCase.isBuiltIn() && testCase.exists()) {
            var builtInBlock = new TestBlock();
            when(builtInBlocks.stream()).thenReturn(Stream.of(builtInBlock));
        } else {
            when(builtInBlocks.stream()).thenReturn(Stream.empty());
        }

        if (!testCase.isBuiltIn() && testCase.exists()) {
            var definition = new ExternalBlockDefinition();
            definition.setId(testCase.blockId());
            definition.setName("External Test");
            definition.setDescription("Test Description");
            var externalBlock = new ExternalContextBlock(definition);
            when(externalBlockLoader.getExternalBlocks()).thenReturn(List.of(externalBlock));
        } else if (!testCase.isBuiltIn()) {
            when(externalBlockLoader.getExternalBlocks()).thenReturn(List.of());
        }

        ResponseEntity<Map<String, Object>> response = controller.getBlockDetails(testCase.blockId());

        if (testCase.exists()) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var body = response.getBody();
            assertThat(body).containsKey("id");
            assertThat(body.get("type")).isEqualTo(testCase.isBuiltIn() ? "built-in" : "external");
        } else {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void getBlockDetails_givenExternalBlock_shouldReturnFullDefinition() {
        var definition = new ExternalBlockDefinition();
        definition.setId("external.detailed");
        definition.setName("Detailed Block");
        definition.setDescription("Detailed Description");
        definition.setRules(List.of("Rule 1", "Rule 2"));
        definition.setExamples(List.of());
        definition.setMetadata(Map.of("key", "value"));
        
        var scoring = new ExternalBlockDefinition.ScoreRules();
        scoring.setBaseScore(0.5);
        definition.setScoring(scoring);
        
        var externalBlock = new ExternalContextBlock(definition);
        when(builtInBlocks.stream()).thenReturn(Stream.empty());
        when(externalBlockLoader.getExternalBlocks()).thenReturn(List.of(externalBlock));

        ResponseEntity<Map<String, Object>> response = controller.getBlockDetails("external.detailed");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body).containsKeys("name", "description", "scoring", "examples", "rules", "metadata");
        assertThat(body.get("name")).isEqualTo("Detailed Block");
        assertThat(body.get("rules")).isEqualTo(List.of("Rule 1", "Rule 2"));
    }

    @Test
    void getBlocksPath_givenConfiguredPath_shouldReturnPathInfo() {
        ResponseEntity<Map<String, Object>> response = controller.getBlocksPath();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body.get("path")).isEqualTo("/test/path");
        assertThat(body).containsKey("exists");
    }

    static class TestBlock implements ContextBlock {
        public static final String EXAMPLE_DATA = "example data";
        
        @Override
        public String id() {
            return "builtin.test";
        }
        
        @Override
        public double score(EndpointInfo info) {
            return 0.5;
        }
        
        @Override
        public String render(EndpointInfo info) {
            return "TEST BLOCK CONTEXT:\n\nRules for your response:\n- Rule 1\n- Rule 2";
        }
    }
}