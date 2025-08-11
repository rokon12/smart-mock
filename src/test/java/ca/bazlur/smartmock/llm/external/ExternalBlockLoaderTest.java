package ca.bazlur.smartmock.llm.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ExternalBlockLoaderTest {

    private ExternalBlockLoader loader;
    private YAMLMapper yamlMapper;
    private ObjectMapper jsonMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        yamlMapper = new YAMLMapper();
        jsonMapper = new ObjectMapper();
        loader = new ExternalBlockLoader(yamlMapper, jsonMapper);
        ReflectionTestUtils.setField(loader, "enabled", true);
        ReflectionTestUtils.setField(loader, "blocksPath", tempDir.toString());
    }

    @Test
    void loadExternalBlocks_givenEmptyDirectory_shouldLoadNoBlocks() {
        loader.loadExternalBlocks();

        assertThat(loader.getExternalBlocks()).isEmpty();
    }

    @Test
    void loadExternalBlocks_givenDisabled_shouldNotLoadBlocks() {
        ReflectionTestUtils.setField(loader, "enabled", false);
        
        loader.loadExternalBlocks();
        
        assertThat(loader.getExternalBlocks()).isEmpty();
    }

    sealed interface BlockFileTestCase {
        String fileName();
        String content();
        
        record YamlBlockCase(String fileName, String content) implements BlockFileTestCase {}
        record JsonBlockCase(String fileName, String content) implements BlockFileTestCase {}
        record InvalidBlockCase(String fileName, String content) implements BlockFileTestCase {}
    }

    static Stream<BlockFileTestCase> blockFileTestCases() {
        return Stream.of(
            new BlockFileTestCase.YamlBlockCase(
                "test-block.yaml",
                """
                id: test.block.v1
                name: Test Block
                description: Test description
                scoring:
                  baseScore: 0.5
                examples:
                  - name: test example
                    json: |
                      {"id": "test123"}
                rules:
                  - Test rule 1
                """
            ),
            new BlockFileTestCase.JsonBlockCase(
                "test-block.json",
                """
                {
                  "id": "test.json.block",
                  "name": "JSON Test Block",
                  "description": "JSON test",
                  "scoring": {
                    "baseScore": 0.3
                  },
                  "examples": [{
                    "name": "json example",
                    "json": "{\\"id\\": \\"json123\\"}"
                  }]
                }
                """
            ),
            new BlockFileTestCase.InvalidBlockCase(
                "invalid.yaml",
                "invalid: [yaml content"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("blockFileTestCases")
    void loadBlockFile_givenVariousFormats_shouldHandleAppropriately(BlockFileTestCase testCase) throws IOException {
        Path blockFile = tempDir.resolve(testCase.fileName());
        Files.writeString(blockFile, testCase.content());
        
        loader.loadExternalBlocks();
        
        switch (testCase) {
            case BlockFileTestCase.YamlBlockCase yaml -> {
                assertThat(loader.getExternalBlocks())
                    .anyMatch(block -> block.id().equals("test.block.v1"));
            }
            case BlockFileTestCase.JsonBlockCase json -> {
                assertThat(loader.getExternalBlocks())
                    .anyMatch(block -> block.id().equals("test.json.block"));
            }
            case BlockFileTestCase.InvalidBlockCase invalid -> {
                assertThatNoException().isThrownBy(() -> loader.loadExternalBlocks());
            }
        }
    }

    @Test
    void reloadBlocks_givenExistingBlocks_shouldClearAndReload() throws IOException {
        Path blockFile = tempDir.resolve("initial.yaml");
        Files.writeString(blockFile, """
            id: initial.block
            name: Initial Block
            """);
        
        loader.loadExternalBlocks();
        int initialSize = loader.getExternalBlocks().size();
        
        Path newBlockFile = tempDir.resolve("new.yaml");
        Files.writeString(newBlockFile, """
            id: new.block
            name: New Block
            """);
        
        loader.reloadBlocks();
        
        assertThat(loader.getExternalBlocks()).hasSizeGreaterThan(initialSize);
        assertThat(loader.getExternalBlocks())
            .anyMatch(block -> block.id().equals("new.block"));
    }

    @Test
    void loadBlockFile_givenNoId_shouldGenerateFromFilename() throws IOException {
        Path blockFile = tempDir.resolve("custom-domain.yaml");
        Files.writeString(blockFile, """
            name: Custom Domain
            description: Test without ID
            """);
        
        loader.loadExternalBlocks();
        
        assertThat(loader.getExternalBlocks())
            .anyMatch(block -> block.id().equals("external.custom-domain"));
    }
}