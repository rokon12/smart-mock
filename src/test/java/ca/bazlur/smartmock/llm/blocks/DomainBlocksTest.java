package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DomainBlocksTest {

    sealed interface DomainBlockTestCase {
        ContextBlock block();
        EndpointInfo matchingEndpoint();
        EndpointInfo nonMatchingEndpoint();
        double expectedHighScore();
        double expectedLowScore();
        String expectedContent();
        
        record HealthcareCase(
            ContextBlock block,
            EndpointInfo matchingEndpoint,
            EndpointInfo nonMatchingEndpoint,
            double expectedHighScore,
            double expectedLowScore,
            String expectedContent
        ) implements DomainBlockTestCase {}
        
        record EducationCase(
            ContextBlock block,
            EndpointInfo matchingEndpoint,
            EndpointInfo nonMatchingEndpoint,
            double expectedHighScore,
            double expectedLowScore,
            String expectedContent
        ) implements DomainBlockTestCase {}
        
        record RealEstateCase(
            ContextBlock block,
            EndpointInfo matchingEndpoint,
            EndpointInfo nonMatchingEndpoint,
            double expectedHighScore,
            double expectedLowScore,
            String expectedContent
        ) implements DomainBlockTestCase {}
        
        record TravelCase(
            ContextBlock block,
            EndpointInfo matchingEndpoint,
            EndpointInfo nonMatchingEndpoint,
            double expectedHighScore,
            double expectedLowScore,
            String expectedContent
        ) implements DomainBlockTestCase {}
        
        record SocialMediaCase(
            ContextBlock block,
            EndpointInfo matchingEndpoint,
            EndpointInfo nonMatchingEndpoint,
            double expectedHighScore,
            double expectedLowScore,
            String expectedContent
        ) implements DomainBlockTestCase {}
        
        record AnalyticsCase(
            ContextBlock block,
            EndpointInfo matchingEndpoint,
            EndpointInfo nonMatchingEndpoint,
            double expectedHighScore,
            double expectedLowScore,
            String expectedContent
        ) implements DomainBlockTestCase {}
    }

    static Stream<DomainBlockTestCase> domainBlockTestCases() {
        return Stream.of(
            new DomainBlockTestCase.HealthcareCase(
                new HealthcareBlock(),
                new EndpointInfo("/api/patients/123", "getPatient", "GET", 
                    "{\"patientId\": \"string\", \"diagnosis\": \"string\"}", "{}"),
                new EndpointInfo("/api/products", "getProducts", "GET", "{}", "{}"),
                0.75,
                0.0,
                "HEALTHCARE"
            ),
            new DomainBlockTestCase.EducationCase(
                new EducationBlock(),
                new EndpointInfo("/api/students/courses", "getStudentCourses", "GET",
                    "{\"studentId\": \"string\", \"courseCode\": \"string\"}", "{}"),
                new EndpointInfo("/api/orders", "getOrders", "GET", "{}", "{}"),
                0.55,
                0.0,
                "EDUCATION"
            ),
            new DomainBlockTestCase.RealEstateCase(
                new RealEstateBlock(),
                new EndpointInfo("/api/properties/listings", "getListings", "GET",
                    "{\"propertyId\": \"string\", \"squareFeet\": \"number\"}", "{}"),
                new EndpointInfo("/api/users", "getUsers", "GET", "{}", "{}"),
                0.55,
                0.0,
                "REAL ESTATE"
            ),
            new DomainBlockTestCase.TravelCase(
                new TravelBlock(),
                new EndpointInfo("/api/flights/bookings", "getBookings", "GET",
                    "{\"flightNumber\": \"string\", \"departure\": \"string\"}", "{}"),
                new EndpointInfo("/api/products", "getProducts", "GET", "{}", "{}"),
                0.75,
                0.0,
                "TRAVEL"
            ),
            new DomainBlockTestCase.SocialMediaCase(
                new SocialMediaBlock(),
                new EndpointInfo("/api/posts/comments", "getComments", "GET",
                    "{\"postId\": \"string\", \"likes\": \"number\"}", "{}"),
                new EndpointInfo("/api/invoices", "getInvoices", "GET", "{}", "{}"),
                0.75,
                0.0,
                "SOCIAL MEDIA"
            ),
            new DomainBlockTestCase.AnalyticsCase(
                new AnalyticsBlock(),
                new EndpointInfo("/api/metrics/dashboard", "getDashboard", "GET",
                    "{\"metricId\": \"string\", \"dataPoints\": \"array\"}", "{}"),
                new EndpointInfo("/api/products", "getProducts", "GET", "{}", "{}"),
                0.55,
                0.0,
                "ANALYTICS"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("domainBlockTestCases")
    void score_givenMatchingEndpoint_shouldReturnHighScore(DomainBlockTestCase testCase) {
        double score = testCase.block().score(testCase.matchingEndpoint());
        
        assertThat(score).isGreaterThanOrEqualTo(testCase.expectedHighScore());
    }

    @ParameterizedTest
    @MethodSource("domainBlockTestCases")
    void score_givenNonMatchingEndpoint_shouldReturnLowScore(DomainBlockTestCase testCase) {
        double score = testCase.block().score(testCase.nonMatchingEndpoint());
        
        assertThat(score).isLessThanOrEqualTo(testCase.expectedLowScore());
    }

    @ParameterizedTest
    @MethodSource("domainBlockTestCases")
    void render_givenEndpoint_shouldContainDomainContext(DomainBlockTestCase testCase) {
        String output = testCase.block().render(testCase.matchingEndpoint());
        
        assertThat(output).contains(testCase.expectedContent());
        assertThat(output).contains("Example");
        assertThat(output).contains("Rules");
    }

    @Test
    void healthcareBlock_givenPatientEndpoint_shouldProvideHealthcareContext() {
        var block = new HealthcareBlock();
        var info = new EndpointInfo("/api/patients", "getPatients", "GET", 
            "{\"patientId\": \"string\"}", "{}");
        
        String output = block.render(info);
        
        assertThat(output)
            .contains("HEALTHCARE")
            .contains("patientId")
            .contains("HIPAA")
            .contains("Patient IDs like");
    }

    @Test
    void educationBlock_givenCourseEndpoint_shouldProvideEducationContext() {
        var block = new EducationBlock();
        var info = new EndpointInfo("/api/courses", "getCourses", "GET",
            "{\"courseCode\": \"string\"}", "{}");
        
        String output = block.render(info);
        
        assertThat(output)
            .contains("EDUCATION")
            .contains("CS-101")
            .contains("Introduction to Computer Science")
            .contains("Student IDs like");
    }

    @Test
    void realEstateBlock_givenPropertyEndpoint_shouldProvideRealEstateContext() {
        var block = new RealEstateBlock();
        var info = new EndpointInfo("/api/properties", "getProperties", "GET",
            "{\"propertyType\": \"string\"}", "{}");
        
        String output = block.render(info);
        
        assertThat(output)
            .contains("REAL ESTATE")
            .contains("propertyId")
            .contains("Square footage")
            .contains("MLS numbers like");
    }

    @Test
    void travelBlock_givenFlightEndpoint_shouldProvideTravelContext() {
        var block = new TravelBlock();
        var info = new EndpointInfo("/api/flights", "getFlights", "GET",
            "{\"flightNumber\": \"string\"}", "{}");
        
        String output = block.render(info);
        
        assertThat(output)
            .contains("TRAVEL")
            .contains("flight")
            .contains("IATA")
            .contains("booking");
    }

    @Test
    void socialMediaBlock_givenPostEndpoint_shouldProvideSocialContext() {
        var block = new SocialMediaBlock();
        var info = new EndpointInfo("/api/posts", "getPosts", "GET",
            "{\"userId\": \"string\"}", "{}");
        
        String output = block.render(info);
        
        assertThat(output)
            .contains("SOCIAL MEDIA")
            .contains("@tech_enthusiast")
            .contains("hashtags")
            .contains("likes");
    }

    @Test
    void analyticsBlock_givenMetricsEndpoint_shouldProvideAnalyticsContext() {
        var block = new AnalyticsBlock();
        var info = new EndpointInfo("/api/metrics", "getMetrics", "GET",
            "{\"metricName\": \"string\"}", "{}");
        
        String output = block.render(info);
        
        assertThat(output)
            .contains("ANALYTICS")
            .contains("period")
            .contains("Percentages")
            .contains("conversion rate");
    }

    @Test
    void allBlocks_shouldHaveUniqueIds() {
        var blocks = Stream.of(
            new HealthcareBlock(),
            new EducationBlock(),
            new RealEstateBlock(),
            new TravelBlock(),
            new SocialMediaBlock(),
            new AnalyticsBlock()
        );
        
        var ids = blocks.map(ContextBlock::id).toList();
        
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(ids).hasSize(6);
    }

    @Test
    void allBlocks_givenEmptySchema_shouldStillProvideContext() {
        var blocks = Stream.of(
            new HealthcareBlock(),
            new EducationBlock(),
            new RealEstateBlock(),
            new TravelBlock(),
            new SocialMediaBlock(),
            new AnalyticsBlock()
        );
        
        var emptyInfo = new EndpointInfo("/api/test", "test", "GET", "{}", "{}");
        
        blocks.forEach(block -> {
            String output = block.render(emptyInfo);
            assertThat(output).isNotEmpty();
            assertThat(output).contains("Example");
        });
    }
}