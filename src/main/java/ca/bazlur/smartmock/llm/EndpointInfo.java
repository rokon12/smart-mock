package ca.bazlur.smartmock.llm;


public record EndpointInfo(
    String path,
    String operationId,
    String method,
    String jsonSchemaMinified,
    String requestContextMinified
) {
}
