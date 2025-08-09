package ca.bazlur.smartmock.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpHeaders;

@Data
@Builder
public class MockResult {
    private int status;
    private String body;
    private HttpHeaders headers;
}