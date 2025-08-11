package ca.bazlur.smartmock.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.media.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSchemaConverterTest {

    private JsonSchemaConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JsonSchemaConverter(new ObjectMapper());
    }

    sealed interface SchemaConversionTestCase {
        Schema<?> schema();
        String expectedJsonType();
        boolean expectsProperties();
        
        record StringSchemaCase(Schema<?> schema) implements SchemaConversionTestCase {
            @Override public String expectedJsonType() { return "string"; }
            @Override public boolean expectsProperties() { return false; }
        }
        
        record NumberSchemaCase(Schema<?> schema) implements SchemaConversionTestCase {
            @Override public String expectedJsonType() { return "number"; }
            @Override public boolean expectsProperties() { return false; }
        }
        
        record IntegerSchemaCase(Schema<?> schema) implements SchemaConversionTestCase {
            @Override public String expectedJsonType() { return "integer"; }
            @Override public boolean expectsProperties() { return false; }
        }
        
        record BooleanSchemaCase(Schema<?> schema) implements SchemaConversionTestCase {
            @Override public String expectedJsonType() { return "boolean"; }
            @Override public boolean expectsProperties() { return false; }
        }
        
        record ArraySchemaCase(Schema<?> schema) implements SchemaConversionTestCase {
            @Override public String expectedJsonType() { return "array"; }
            @Override public boolean expectsProperties() { return false; }
        }
        
        record ObjectSchemaCase(Schema<?> schema) implements SchemaConversionTestCase {
            @Override public String expectedJsonType() { return "object"; }
            @Override public boolean expectsProperties() { return true; }
        }
    }

    static Stream<SchemaConversionTestCase> schemaConversionTestCases() {
        var stringSchema = new StringSchema();
        stringSchema.setMinLength(1);
        stringSchema.setMaxLength(100);
        
        var numberSchema = new NumberSchema();
        numberSchema.setMinimum(new BigDecimal("0"));
        numberSchema.setMaximum(new BigDecimal("1000"));
        
        var integerSchema = new IntegerSchema();
        integerSchema.setMinimum(new BigDecimal("1"));
        integerSchema.setMaximum(new BigDecimal("100"));
        
        var booleanSchema = new BooleanSchema();
        
        var arraySchema = new ArraySchema();
        arraySchema.setItems(new StringSchema());
        arraySchema.setMinItems(1);
        arraySchema.setMaxItems(10);
        
        var objectSchema = new ObjectSchema();
        objectSchema.addProperty("id", new StringSchema());
        objectSchema.addProperty("name", new StringSchema());
        objectSchema.addProperty("age", new IntegerSchema());
        
        return Stream.of(
            new SchemaConversionTestCase.StringSchemaCase(stringSchema),
            new SchemaConversionTestCase.NumberSchemaCase(numberSchema),
            new SchemaConversionTestCase.IntegerSchemaCase(integerSchema),
            new SchemaConversionTestCase.BooleanSchemaCase(booleanSchema),
            new SchemaConversionTestCase.ArraySchemaCase(arraySchema),
            new SchemaConversionTestCase.ObjectSchemaCase(objectSchema)
        );
    }

    @ParameterizedTest
    @MethodSource("schemaConversionTestCases")
    void convertToJsonSchema_givenVariousSchemas_shouldConvertCorrectly(SchemaConversionTestCase testCase) {
        String jsonSchema = converter.convertToJsonSchema(testCase.schema());
        
        assertThat(jsonSchema).contains("\"type\" : \"" + testCase.expectedJsonType() + "\"");
        
        if (testCase.expectsProperties()) {
            assertThat(jsonSchema).contains("\"properties\"");
        }
    }

    @Test
    void convertToJsonSchema_givenObjectWithProperties_shouldIncludeAllProperties() {
        var schema = new ObjectSchema();
        schema.addProperty("firstName", new StringSchema());
        schema.addProperty("lastName", new StringSchema());
        schema.addProperty("email", new StringSchema().format("email"));
        schema.addProperty("age", new IntegerSchema());
        schema.addProperty("active", new BooleanSchema());
        
        String jsonSchema = converter.convertToJsonSchema(schema);
        
        assertThat(jsonSchema)
            .contains("\"firstName\"")
            .contains("\"lastName\"")
            .contains("\"email\"")
            .contains("\"age\"")
            .contains("\"active\"")
            .contains("\"type\" : \"object\"");
    }

    @Test
    void convertToJsonSchema_givenArrayWithItems_shouldIncludeItemsSchema() {
        var itemSchema = new ObjectSchema();
        itemSchema.addProperty("id", new StringSchema());
        itemSchema.addProperty("value", new NumberSchema());
        
        var arraySchema = new ArraySchema();
        arraySchema.setItems(itemSchema);
        
        String jsonSchema = converter.convertToJsonSchema(arraySchema);
        
        assertThat(jsonSchema)
            .contains("\"type\" : \"array\"")
            .contains("\"items\"")
            .contains("\"properties\"");
    }

    @Test
    void convertToJsonSchema_givenSchemaWithFormat_shouldIncludeFormat() {
        var schema = new StringSchema();
        schema.setFormat("date-time");
        
        String jsonSchema = converter.convertToJsonSchema(schema);
        
        assertThat(jsonSchema).contains("\"format\" : \"date-time\"");
    }

    @Test
    void convertToJsonSchema_givenSchemaWithEnum_shouldIncludeEnum() {
        var schema = new StringSchema();
        schema.setEnum(java.util.List.of("active", "inactive", "pending"));
        
        String jsonSchema = converter.convertToJsonSchema(schema);
        
        assertThat(jsonSchema)
            .contains("\"enum\"")
            .contains("\"active\"")
            .contains("\"inactive\"")
            .contains("\"pending\"");
    }

    @Test
    void convertToJsonSchema_givenSchemaWithRequired_shouldIncludeRequired() {
        var schema = new ObjectSchema();
        schema.addProperty("id", new StringSchema());
        schema.addProperty("name", new StringSchema());
        schema.setRequired(java.util.List.of("id", "name"));
        
        String jsonSchema = converter.convertToJsonSchema(schema);
        
        assertThat(jsonSchema)
            .contains("\"required\"")
            .contains("[ \"id\", \"name\" ]");
    }

    @Test
    void convertToJsonSchema_givenNestedObjects_shouldHandleNesting() {
        var addressSchema = new ObjectSchema();
        addressSchema.addProperty("street", new StringSchema());
        addressSchema.addProperty("city", new StringSchema());
        addressSchema.addProperty("zipCode", new StringSchema());
        
        var personSchema = new ObjectSchema();
        personSchema.addProperty("name", new StringSchema());
        personSchema.addProperty("address", addressSchema);
        
        String jsonSchema = converter.convertToJsonSchema(personSchema);
        
        assertThat(jsonSchema)
            .contains("\"address\"")
            .contains("\"street\"")
            .contains("\"city\"")
            .contains("\"zipCode\"");
    }

    @Test
    void convertToJsonSchema_givenNullSchema_shouldReturnEmptyObject() {
        String jsonSchema = converter.convertToJsonSchema(null);
        
        assertThat(jsonSchema).contains("\"$schema\" : \"http://json-schema.org/draft-07/schema#\"");
        assertThat(jsonSchema).doesNotContain("\"type\"");
        assertThat(jsonSchema).doesNotContain("\"properties\"");
    }

    @Test
    void convertToJsonSchema_givenSchemaWithValidation_shouldIncludeConstraints() {
        var schema = new StringSchema();
        schema.setMinLength(5);
        schema.setMaxLength(50);
        schema.setPattern("^[A-Za-z]+$");
        
        String jsonSchema = converter.convertToJsonSchema(schema);
        
        assertThat(jsonSchema)
            .contains("\"minLength\" : 5")
            .contains("\"maxLength\" : 50")
            .contains("\"pattern\" : \"^[A-Za-z]+$\"");
    }

    @Test
    void convertToJsonSchema_givenSchemaMinified_shouldReturnMinifiedJson() {
        var schema = new ObjectSchema();
        schema.addProperty("id", new StringSchema());
        schema.addProperty("name", new StringSchema());
        
        String jsonSchema = converter.convertToJsonSchema(schema);
        String minified = jsonSchema.replaceAll("\\s+", "");
        
        assertThat(minified).doesNotContain("\n");
        assertThat(minified).doesNotContain("  ");
        assertThat(minified).contains("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"");
    }

    @Test
    void convertToJsonSchema_givenComposedSchema_shouldHandleAllOf() {
        var baseSchema = new ObjectSchema();
        baseSchema.addProperty("id", new StringSchema());
        
        var extendedSchema = new ObjectSchema();
        extendedSchema.addProperty("name", new StringSchema());
        
        var composedSchema = new ComposedSchema();
        composedSchema.addAllOfItem(baseSchema);
        composedSchema.addAllOfItem(extendedSchema);
        
        String jsonSchema = converter.convertToJsonSchema(composedSchema);
        
        assertThat(jsonSchema)
            .contains("\"allOf\"")
            .contains("\"id\"")
            .contains("\"name\"");
    }
}