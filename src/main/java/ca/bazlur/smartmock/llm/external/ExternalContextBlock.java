package ca.bazlur.smartmock.llm.external;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class ExternalContextBlock implements ContextBlock {
    
    @Getter
    private final ExternalBlockDefinition definition;
    
    @Override
    public String id() {
        return definition.getId();
    }
    
    @Override
    public double score(EndpointInfo info) {
        double totalScore = 0.0;
        
        String path = safe(info.path());
        String operationId = safe(info.operationId());
        String jsonSchema = safe(info.jsonSchemaMinified());
        
        if (definition.getScoring() != null) {
            var scoring = definition.getScoring();
            
            if (scoring.getBaseScore() != null) {
                totalScore = scoring.getBaseScore();
            }
            
            if (scoring.getPathPatterns() != null) {
                for (var pathPattern : scoring.getPathPatterns()) {
                    if (matches(path, pathPattern.getPattern())) {
                        totalScore += pathPattern.getScore();
                    }
                }
            }
            
            if (scoring.getOperationPatterns() != null) {
                for (var opPattern : scoring.getOperationPatterns()) {
                    if (matches(operationId, opPattern.getPattern())) {
                        totalScore += opPattern.getScore();
                    }
                }
            }
            
            if (scoring.getSchemaPatterns() != null) {
                for (var schemaPattern : scoring.getSchemaPatterns()) {
                    if (matches(jsonSchema, schemaPattern.getPattern())) {
                        totalScore += schemaPattern.getScore();
                    }
                }
            }
        }
        
        return Math.min(1.0, Math.max(0.0, totalScore));
    }
    
    @Override
    public String render(EndpointInfo info) {
        StringBuilder sb = new StringBuilder();
        
        if (definition.getName() != null) {
            sb.append(definition.getName().toUpperCase()).append(" CONTEXT:\n");
        }
        
        String path = info.path().toLowerCase();
        String method = info.method().toUpperCase();
        
        if (definition.getExamples() != null && !definition.getExamples().isEmpty()) {
            for (var example : definition.getExamples()) {
                if (matchesCondition(example.getCondition(), path, method)) {
                    sb.append("Example ").append(example.getName()).append(":\n");
                    sb.append(example.getJson()).append("\n");
                    break;
                }
            }
            
            if (sb.length() == 0 || !sb.toString().contains("Example")) {
                var firstExample = definition.getExamples().get(0);
                sb.append("Example ").append(firstExample.getName()).append(":\n");
                sb.append(firstExample.getJson()).append("\n");
            }
        }
        
        if (definition.getRules() != null && !definition.getRules().isEmpty()) {
            sb.append("\nRules for your response:\n");
            for (String rule : definition.getRules()) {
                sb.append("- ").append(rule).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private boolean matches(String text, String pattern) {
        if (text == null || pattern == null) return false;
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
        } catch (Exception e) {
            log.debug("Pattern matching failed for pattern: {}", pattern, e);
            return false;
        }
    }
    
    private boolean matchesCondition(String condition, String path, String method) {
        if (condition == null || condition.isBlank()) return true;
        
        try {
            condition = condition.toLowerCase();
            
            if (condition.contains("path:")) {
                String pathPattern = condition.substring(condition.indexOf("path:") + 5).trim();
                return path.contains(pathPattern);
            }
            
            if (condition.contains("method:")) {
                String methodPattern = condition.substring(condition.indexOf("method:") + 7).trim();
                return method.equalsIgnoreCase(methodPattern);
            }
            
            return path.contains(condition) || method.equalsIgnoreCase(condition);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private String safe(String s) {
        return s == null ? "" : s.toLowerCase();
    }
}