package ca.bazlur.smartmock.llm.external;

import ca.bazlur.smartmock.llm.EndpointInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalContextBlockTest {

    private ExternalBlockDefinition definition;
    private ExternalContextBlock block;

    @BeforeEach
    void setUp() {
        definition = new ExternalBlockDefinition();
        definition.setId("test.block");
        definition.setName("Test Block");
        definition.setDescription("Test Description");
    }

    @Test
    void id_givenDefinitionWithId_shouldReturnId() {
        block = new ExternalContextBlock(definition);
        
        assertThat(block.id()).isEqualTo("test.block");
    }

    sealed interface ScoringTestCase {
        ExternalBlockDefinition.ScoreRules scoring();
        EndpointInfo endpointInfo();
        double expectedScore();
        
        record PathMatchCase(
            ExternalBlockDefinition.ScoreRules scoring,
            EndpointInfo endpointInfo,
            double expectedScore
        ) implements ScoringTestCase {}
        
        record OperationMatchCase(
            ExternalBlockDefinition.ScoreRules scoring,
            EndpointInfo endpointInfo,
            double expectedScore
        ) implements ScoringTestCase {}
        
        record SchemaMatchCase(
            ExternalBlockDefinition.ScoreRules scoring,
            EndpointInfo endpointInfo,
            double expectedScore
        ) implements ScoringTestCase {}
        
        record NoMatchCase(
            ExternalBlockDefinition.ScoreRules scoring,
            EndpointInfo endpointInfo,
            double expectedScore
        ) implements ScoringTestCase {}
    }

    static Stream<ScoringTestCase> scoringTestCases() {
        var pathScoring = new ExternalBlockDefinition.ScoreRules();
        pathScoring.setBaseScore(0.2);
        var pathPattern = new ExternalBlockDefinition.PathPattern();
        pathPattern.setPattern("products");
        pathPattern.setScore(0.3);
        pathScoring.setPathPatterns(List.of(pathPattern));
        
        var operationScoring = new ExternalBlockDefinition.ScoreRules();
        var opPattern = new ExternalBlockDefinition.OperationPattern();
        opPattern.setPattern("getProducts");
        opPattern.setScore(0.4);
        operationScoring.setOperationPatterns(List.of(opPattern));
        
        var schemaScoring = new ExternalBlockDefinition.ScoreRules();
        var schemaPattern = new ExternalBlockDefinition.SchemaPattern();
        schemaPattern.setPattern("productId");
        schemaPattern.setScore(0.5);
        schemaScoring.setSchemaPatterns(List.of(schemaPattern));
        
        return Stream.of(
            new ScoringTestCase.PathMatchCase(
                pathScoring,
                new EndpointInfo("/api/products", "listProducts", "GET", "{}", "{}"),
                0.5
            ),
            new ScoringTestCase.OperationMatchCase(
                operationScoring,
                new EndpointInfo("/api/items", "getProducts", "GET", "{}", "{}"),
                0.4
            ),
            new ScoringTestCase.SchemaMatchCase(
                schemaScoring,
                new EndpointInfo("/api/data", "getData", "GET", "{\"productId\": \"string\"}", "{}"),
                0.5
            ),
            new ScoringTestCase.NoMatchCase(
                pathScoring,
                new EndpointInfo("/api/users", "getUsers", "GET", "{}", "{}"),
                0.2
            )
        );
    }

    @ParameterizedTest
    @MethodSource("scoringTestCases")
    void score_givenVariousPatterns_shouldCalculateCorrectScore(ScoringTestCase testCase) {
        definition.setScoring(testCase.scoring());
        block = new ExternalContextBlock(definition);
        
        double score = block.score(testCase.endpointInfo());
        
        assertThat(score).isEqualTo(testCase.expectedScore());
    }

    @Test
    void score_givenScoreAboveOne_shouldCapAtOne() {
        var scoring = new ExternalBlockDefinition.ScoreRules();
        scoring.setBaseScore(0.8);
        var pattern = new ExternalBlockDefinition.PathPattern();
        pattern.setPattern("test");
        pattern.setScore(0.5);
        scoring.setPathPatterns(List.of(pattern));
        definition.setScoring(scoring);
        block = new ExternalContextBlock(definition);
        
        var info = new EndpointInfo("/test", "test", "GET", "{}", "{}");
        double score = block.score(info);
        
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void score_givenNullScoring_shouldReturnZero() {
        definition.setScoring(null);
        block = new ExternalContextBlock(definition);
        
        var info = new EndpointInfo("/test", "test", "GET", "{}", "{}");
        double score = block.score(info);
        
        assertThat(score).isEqualTo(0.0);
    }

    sealed interface RenderTestCase {
        ExternalBlockDefinition definition();
        EndpointInfo endpointInfo();
        String expectedContent();
        
        record WithExamplesCase(
            ExternalBlockDefinition definition,
            EndpointInfo endpointInfo,
            String expectedContent
        ) implements RenderTestCase {}
        
        record WithRulesCase(
            ExternalBlockDefinition definition,
            EndpointInfo endpointInfo,
            String expectedContent
        ) implements RenderTestCase {}
        
        record WithConditionMatchCase(
            ExternalBlockDefinition definition,
            EndpointInfo endpointInfo,
            String expectedContent
        ) implements RenderTestCase {}
    }

    static Stream<RenderTestCase> renderTestCases() {
        var defWithExamples = new ExternalBlockDefinition();
        defWithExamples.setName("Test");
        var example = new ExternalBlockDefinition.Example();
        example.setName("Example 1");
        example.setJson("{\"test\": \"data\"}");
        defWithExamples.setExamples(List.of(example));
        
        var defWithRules = new ExternalBlockDefinition();
        defWithRules.setName("Rules");
        defWithRules.setRules(List.of("Rule 1", "Rule 2"));
        
        var defWithCondition = new ExternalBlockDefinition();
        defWithCondition.setName("Conditional");
        var condExample1 = new ExternalBlockDefinition.Example();
        condExample1.setName("Product Example");
        condExample1.setCondition("path:product");
        condExample1.setJson("{\"product\": true}");
        var condExample2 = new ExternalBlockDefinition.Example();
        condExample2.setName("Default Example");
        condExample2.setJson("{\"default\": true}");
        defWithCondition.setExamples(List.of(condExample1, condExample2));
        
        return Stream.of(
            new RenderTestCase.WithExamplesCase(
                defWithExamples,
                new EndpointInfo("/test", "test", "GET", "{}", "{}"),
                "Example 1"
            ),
            new RenderTestCase.WithRulesCase(
                defWithRules,
                new EndpointInfo("/test", "test", "GET", "{}", "{}"),
                "Rules for your response"
            ),
            new RenderTestCase.WithConditionMatchCase(
                defWithCondition,
                new EndpointInfo("/api/products", "getProducts", "GET", "{}", "{}"),
                "Product Example"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("renderTestCases")
    void render_givenVariousDefinitions_shouldGenerateCorrectOutput(RenderTestCase testCase) {
        block = new ExternalContextBlock(testCase.definition());
        
        String output = block.render(testCase.endpointInfo());
        
        assertThat(output).contains(testCase.expectedContent());
    }

    @Test
    void render_givenMethodCondition_shouldMatchCorrectly() {
        var example = new ExternalBlockDefinition.Example();
        example.setName("POST Example");
        example.setCondition("method:POST");
        example.setJson("{\"method\": \"POST\"}");
        definition.setExamples(List.of(example));
        block = new ExternalContextBlock(definition);
        
        var info = new EndpointInfo("/api/test", "test", "POST", "{}", "{}");
        String output = block.render(info);
        
        assertThat(output).contains("POST Example");
    }

    @Test
    void getDefinition_givenDefinition_shouldReturnSameDefinition() {
        block = new ExternalContextBlock(definition);
        
        assertThat(block.getDefinition()).isSameAs(definition);
    }
}