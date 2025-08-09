package ca.bazlur.smartmock.model;

import io.swagger.v3.oas.models.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class Plan {
    private Scenario scenario;
    private int statusCode;
    private Schema<?> responseSchema;
    private String jsonSchema;
    private Map<String, Object> requestContext;
    private String operationId;
    private String path;
    private String method;
}