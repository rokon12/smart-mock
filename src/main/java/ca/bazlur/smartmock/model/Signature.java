package ca.bazlur.smartmock.model;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder
public class Signature {
    private String method;
    private String path;
    private String queryParams;
    private String body;
    private String scenario;
    private String seed;

    public static Signature from(HttpServletRequest request, String body) {
        String scenario = request.getHeader("X-Mock-Scenario");
        String seed = request.getHeader("X-Mock-Seed");
        
        return Signature.builder()
                .method(request.getMethod())
                .path(request.getRequestURI().replace("/mock", ""))
                .queryParams(request.getQueryString())
                .body(body)
                .scenario(scenario != null ? scenario : "happy")
                .seed(seed != null ? seed : "default")
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Signature signature = (Signature) o;
        return Objects.equals(method, signature.method) &&
                Objects.equals(path, signature.path) &&
                Objects.equals(queryParams, signature.queryParams) &&
                Objects.equals(body, signature.body) &&
                Objects.equals(scenario, signature.scenario) &&
                Objects.equals(seed, signature.seed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, queryParams, body, scenario, seed);
    }
}