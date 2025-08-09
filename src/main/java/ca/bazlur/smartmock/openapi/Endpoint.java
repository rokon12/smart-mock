package ca.bazlur.smartmock.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Endpoint {
    private String path;
    private String method;
    private Operation operation;
    private List<Parameter> parameters;
    private ApiResponses responses;

    public String getOperationId() {
        return operation != null ? operation.getOperationId() : null;
    }

    public String getSummary() {
        return operation != null ? operation.getSummary() : null;
    }
}