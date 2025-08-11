package ca.bazlur.smartmock.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RequestResponseCorrelatorTest {

  private RequestResponseCorrelator correlator;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    correlator = new RequestResponseCorrelator(objectMapper);
  }

  sealed interface CorrelationTestCase {
    Map<String, Object> requestContext();

    String expectedContent();

    record GetByIdCase(
        Map<String, Object> requestContext,
        String expectedContent
    ) implements CorrelationTestCase {
    }

    record PostCreateCase(
        Map<String, Object> requestContext,
        String expectedContent
    ) implements CorrelationTestCase {
    }

    record PutUpdateCase(
        Map<String, Object> requestContext,
        String expectedContent
    ) implements CorrelationTestCase {
    }

    record DeleteCase(
        Map<String, Object> requestContext,
        String expectedContent
    ) implements CorrelationTestCase {
    }

    record ListCase(
        Map<String, Object> requestContext,
        String expectedContent
    ) implements CorrelationTestCase {
    }
  }

  static Stream<CorrelationTestCase> correlationTestCases() {
    return Stream.of(
        new CorrelationTestCase.GetByIdCase(
            Map.of(
                "method", "GET",
                "path", "/api/users/123",
                "pathParameters", Map.of("id", "123")
            ),
            "123"
        ),
        new CorrelationTestCase.PostCreateCase(
            Map.of(
                "method", "POST",
                "path", "/api/users",
                "body", Map.of("name", "John", "email", "john@example.com")
            ),
            "John"
        ),
        new CorrelationTestCase.PutUpdateCase(
            Map.of(
                "method", "PUT",
                "path", "/api/users/456",
                "pathParameters", Map.of("id", "456"),
                "body", Map.of("name", "Jane Updated")
            ),
            "456"
        ),
        new CorrelationTestCase.DeleteCase(
            Map.of(
                "method", "DELETE",
                "path", "/api/users/789",
                "pathParameters", Map.of("id", "789")
            ),
            "789"
        ),
        new CorrelationTestCase.ListCase(
            Map.of(
                "method", "GET",
                "path", "/api/users",
                "queryParameters", Map.of("page", "2", "size", "10")
            ),
            "10 items"
        )
    );
  }

  @ParameterizedTest
  @MethodSource("correlationTestCases")
  void generateCorrelations_givenVariousRequests_shouldGenerateAppropriateGuidelines(CorrelationTestCase testCase) {
    String correlation = correlator.generateCorrelations(testCase.requestContext());

    assertThat(correlation).contains(testCase.expectedContent());
  }

  @Test
  void generateCorrelations_givenGetWithId_shouldExtractAndSuggestId() {
    Map<String, Object> context = Map.of(
        "method", "GET",
        "path", "/api/products/prod-12345",
        "pathParameters", Map.of("id", "prod-12345")
    );

    String correlation = correlator.generateCorrelations(context);

    assertThat(correlation)
        .contains("prod-12345")
        .contains("REQUEST-RESPONSE CORRELATIONS");
  }

  @Test
  void generateCorrelations_givenPostWithBody_shouldIncludeRequestData() {
    Map<String, Object> context = new HashMap<>();
    context.put("method", "POST");
    context.put("path", "/api/products");
    context.put("body", Map.of(
        "title", "New Product",
        "price", 99.99,
        "category", "Electronics"
    ));

    String correlation = correlator.generateCorrelations(context);

    assertThat(correlation)
        .contains("New Product")
        .contains("REQUEST-RESPONSE CORRELATIONS");
  }

  @Test
  void generateCorrelations_givenPutWithIdAndBody_shouldMergeData() {
    Map<String, Object> context = new HashMap<>();
    context.put("method", "PUT");
    context.put("path", "/api/products/123");
    context.put("pathParameters", Map.of("id", "123"));
    context.put("body", Map.of(
        "price", 149.99,
        "inStock", true
    ));

    String correlation = correlator.generateCorrelations(context);

    assertThat(correlation)
        .contains("123")
        .contains("REQUEST-RESPONSE CORRELATIONS");
  }

  @Test
  void generateCorrelations_givenPatchRequest_shouldSuggestPartialUpdate() {
    Map<String, Object> context = Map.of(
        "method", "PATCH",
        "path", "/api/users/456",
        "pathParameters", Map.of("id", "456"),
        "body", Map.of("status", "active")
    );

    String correlation = correlator.generateCorrelations(context);

    assertThat(correlation)
        .contains("456")
        .contains("REQUEST-RESPONSE CORRELATIONS");
  }

  @Test
  void generateCorrelations_givenDeleteRequest_shouldSuggestConfirmation() {
    Map<String, Object> context = Map.of(
        "method", "DELETE",
        "path", "/api/orders/789",
        "pathParameters", Map.of("id", "789")
    );

    String correlation = correlator.generateCorrelations(context);

    assertThat(correlation)
        .contains("789")
        .contains("REQUEST-RESPONSE CORRELATIONS");
  }

  @Test
  void generateCorrelations_givenListWithQueryParams_shouldSuggestPagination() {
    Map<String, Object> context = Map.of(
        "method", "GET",
        "path", "/api/items",
        "queryParameters", Map.of("page", "3", "size", "20", "sort", "name")
    );

    String correlation = correlator.generateCorrelations(context);

    assertThat(correlation)
        .contains("20")
        .contains("REQUEST-RESPONSE CORRELATIONS");
  }

  @Test
  void generateCorrelations_givenSearchEndpoint_shouldSuggestFilteredResults() {
    Map<String, Object> context = Map.of(
        "method", "GET",
        "path", "/api/search",
        "queryParameters", Map.of("q", "laptop", "category", "electronics")
    );

    String correlation = correlator.generateCorrelations(context);

    assertThat(correlation)
        .contains("laptop")
        .contains("REQUEST-RESPONSE CORRELATIONS");
  }

  @Test
  void generateCorrelations_givenNullInputs_shouldHandleGracefully() {
    String correlation = correlator.generateCorrelations((Map<String, Object>) null);

    assertThat(correlation).isNotNull();
    assertThat(correlation).isEmpty();
  }

  @Test
  void generateCorrelations_givenEmptyContext_shouldReturnEmpty() {
    Map<String, Object> context = new HashMap<>();

    String correlation = correlator.generateCorrelations(context);

    assertThat(correlation).isNotNull();
    assertThat(correlation).isEmpty();
  }
}