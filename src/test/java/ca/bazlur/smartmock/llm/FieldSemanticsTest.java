package ca.bazlur.smartmock.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FieldSemanticsTest {

    private FieldSemantics fieldSemantics;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        fieldSemantics = new FieldSemantics(objectMapper);
    }

    sealed interface AnalyzeTestCase {
        String jsonSchema();
        boolean expectsEmail();
        boolean expectsPhone();
        boolean expectsUrl();
        boolean expectsDate();
        boolean expectsCurrency();
        
        record EmailFieldCase(String jsonSchema) implements AnalyzeTestCase {
            @Override public boolean expectsEmail() { return true; }
            @Override public boolean expectsPhone() { return false; }
            @Override public boolean expectsUrl() { return false; }
            @Override public boolean expectsDate() { return false; }
            @Override public boolean expectsCurrency() { return false; }
        }
        
        record PhoneFieldCase(String jsonSchema) implements AnalyzeTestCase {
            @Override public boolean expectsEmail() { return false; }
            @Override public boolean expectsPhone() { return true; }
            @Override public boolean expectsUrl() { return false; }
            @Override public boolean expectsDate() { return false; }
            @Override public boolean expectsCurrency() { return false; }
        }
        
        record UrlFieldCase(String jsonSchema) implements AnalyzeTestCase {
            @Override public boolean expectsEmail() { return false; }
            @Override public boolean expectsPhone() { return false; }
            @Override public boolean expectsUrl() { return true; }
            @Override public boolean expectsDate() { return false; }
            @Override public boolean expectsCurrency() { return false; }
        }
        
        record DateFieldCase(String jsonSchema) implements AnalyzeTestCase {
            @Override public boolean expectsEmail() { return false; }
            @Override public boolean expectsPhone() { return false; }
            @Override public boolean expectsUrl() { return false; }
            @Override public boolean expectsDate() { return true; }
            @Override public boolean expectsCurrency() { return false; }
        }
        
        record CurrencyFieldCase(String jsonSchema) implements AnalyzeTestCase {
            @Override public boolean expectsEmail() { return false; }
            @Override public boolean expectsPhone() { return false; }
            @Override public boolean expectsUrl() { return false; }
            @Override public boolean expectsDate() { return false; }
            @Override public boolean expectsCurrency() { return true; }
        }
        
        record NoSpecialFieldCase(String jsonSchema) implements AnalyzeTestCase {
            @Override public boolean expectsEmail() { return false; }
            @Override public boolean expectsPhone() { return false; }
            @Override public boolean expectsUrl() { return false; }
            @Override public boolean expectsDate() { return false; }
            @Override public boolean expectsCurrency() { return false; }
        }
    }

    static Stream<AnalyzeTestCase> analyzeTestCases() {
        return Stream.of(
            new AnalyzeTestCase.EmailFieldCase("{\"properties\": {\"email\": {\"type\": \"string\"}, \"userEmail\": {\"type\": \"string\"}}}"),
            new AnalyzeTestCase.PhoneFieldCase("{\"properties\": {\"phone\": {\"type\": \"string\"}, \"phoneNumber\": {\"type\": \"string\"}}}"),
            new AnalyzeTestCase.UrlFieldCase("{\"properties\": {\"url\": {\"type\": \"string\"}, \"websiteUrl\": {\"type\": \"string\"}, \"homepage\": {\"type\": \"string\"}}}"),
            new AnalyzeTestCase.DateFieldCase("{\"properties\": {\"createdAt\": {\"type\": \"string\"}, \"updatedDate\": {\"type\": \"string\"}, \"timestamp\": {\"type\": \"string\"}}}"),
            new AnalyzeTestCase.CurrencyFieldCase("{\"properties\": {\"price\": {\"type\": \"number\"}, \"amount\": {\"type\": \"number\"}, \"total\": {\"type\": \"number\"}}}"),
            new AnalyzeTestCase.NoSpecialFieldCase("{\"properties\": {\"name\": {\"type\": \"string\"}, \"description\": {\"type\": \"string\"}}}")
        );
    }

    @ParameterizedTest
    @MethodSource("analyzeTestCases")
    void analyzeSchema_givenVariousSchemas_shouldIdentifyCorrectFields(AnalyzeTestCase testCase) {
        String guidelines = fieldSemantics.analyzeSchema(testCase.jsonSchema());
        
        if (testCase.expectsEmail()) {
            assertThat(guidelines).contains("Use realistic email");
        }
        if (testCase.expectsPhone()) {
            assertThat(guidelines).contains("Use E.164 format");
        }
        if (testCase.expectsUrl()) {
            assertThat(guidelines).contains("Use valid URLs");
        }
        if (testCase.expectsDate()) {
            assertThat(guidelines).contains("Use ISO-8601 format");
        }
        if (testCase.expectsCurrency()) {
            assertThat(guidelines).contains("Use realistic prices");
        }
    }

    @Test
    void analyzeSchema_givenComplexSchema_shouldIdentifyMultipleFieldTypes() {
        String schema = """
            {
                "properties": {
                    "id": {"type": "string"},
                    "email": {"type": "string"},
                    "phone": {"type": "string"},
                    "website": {"type": "string"},
                    "createdAt": {"type": "string"},
                    "price": {"type": "number"},
                    "description": {"type": "string"}
                }
            }
            """;
        
        String guidelines = fieldSemantics.analyzeSchema(schema);
        
        assertThat(guidelines)
            .contains("FIELD-SPECIFIC SEMANTIC RULES")
            .contains("email")
            .contains("phone")
            .contains("website")
            .contains("createdAt")
            .contains("price");
    }

    @Test
    void analyzeSchema_givenEmptySchema_shouldReturnEmptyGuidelines() {
        String guidelines = fieldSemantics.analyzeSchema("{}");
        
        assertThat(guidelines).isEmpty();
    }

    @Test
    void analyzeSchema_givenNullSchema_shouldReturnEmptyGuidelines() {
        String guidelines = fieldSemantics.analyzeSchema(null);
        
        assertThat(guidelines).isEmpty();
    }

    @Test
    void analyzeSchema_givenAddressFields_shouldSuggestAddressFormat() {
        String schema = "{\"properties\": {\"address\": {\"type\": \"string\"}, \"streetAddress\": {\"type\": \"string\"}, \"zipCode\": {\"type\": \"string\"}}}";
        
        String guidelines = fieldSemantics.analyzeSchema(schema);
        
        assertThat(guidelines).contains("real street names");
    }

    @Test
    void analyzeSchema_givenIdFields_shouldSuggestUuidFormat() {
        String schema = "{\"properties\": {\"id\": {\"type\": \"string\"}, \"userId\": {\"type\": \"string\"}, \"productId\": {\"type\": \"string\"}}}";
        
        String guidelines = fieldSemantics.analyzeSchema(schema);
        
        assertThat(guidelines).contains("UUID");
    }

    @Test
    void analyzeSchema_givenPercentageFields_shouldSuggestPercentageFormat() {
        String schema = "{\"properties\": {\"percentage\": {\"type\": \"number\"}, \"discountPercent\": {\"type\": \"number\"}, \"rate\": {\"type\": \"number\"}}}";
        
        String guidelines = fieldSemantics.analyzeSchema(schema);
        
        assertThat(guidelines).contains("percentage");
    }

    @Test
    void analyzeSchema_givenStatusFields_shouldSuggestEnumValues() {
        String schema = "{\"properties\": {\"status\": {\"type\": \"string\"}, \"orderStatus\": {\"type\": \"string\"}, \"state\": {\"type\": \"string\"}}}";
        
        String guidelines = fieldSemantics.analyzeSchema(schema);
        
        assertThat(guidelines).contains("status");
    }
}