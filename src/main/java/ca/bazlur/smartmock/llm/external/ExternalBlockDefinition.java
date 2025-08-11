package ca.bazlur.smartmock.llm.external;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ExternalBlockDefinition {
    private String id;
    private String name;
    private String description;
    private ScoreRules scoring;
    private List<Example> examples;
    private List<String> rules;
    private Map<String, Object> metadata;
    
    @Data
    public static class ScoreRules {
        private List<PathPattern> pathPatterns;
        private List<OperationPattern> operationPatterns;
        private List<SchemaPattern> schemaPatterns;
        private Double baseScore;
    }
    
    @Data
    public static class PathPattern {
        private String pattern;
        private Double score;
    }
    
    @Data
    public static class OperationPattern {
        private String pattern;
        private Double score;
    }
    
    @Data
    public static class SchemaPattern {
        private String pattern;
        private Double score;
    }
    
    @Data
    public static class Example {
        private String name;
        private String condition;
        private String json;
    }
}