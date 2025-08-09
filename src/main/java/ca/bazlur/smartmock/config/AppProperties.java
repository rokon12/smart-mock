package ca.bazlur.smartmock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String version;
    private String name;
    private String description;
    private Dependencies dependencies;
    private Java java;
    
    @Data
    public static class Dependencies {
        private String springBoot;
        private String langchain4j;
        private String caffeine;
        private String swaggerParser;
        private String jsonSchemaValidator;
    }
    
    @Data
    public static class Java {
        private String version;
        private String source;
    }
}