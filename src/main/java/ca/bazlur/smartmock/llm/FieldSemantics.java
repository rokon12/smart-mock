package ca.bazlur.smartmock.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class FieldSemantics {
    
    private final ObjectMapper objectMapper;
    
    private static final Pattern ID_PATTERN = Pattern.compile(".*(_id|id|Id|ID|identifier|uuid|guid)$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(".*(email|Email|mail|Mail).*");
    private static final Pattern PHONE_PATTERN = Pattern.compile(".*(phone|Phone|mobile|Mobile|tel|Tel|contact).*");
    private static final Pattern NAME_PATTERN = Pattern.compile(".*(name|Name|title|Title|label|Label).*");
    private static final Pattern DATE_PATTERN = Pattern.compile(".*(date|Date|time|Time|At|at|created|updated|modified|birth|dob).*");
    private static final Pattern PRICE_PATTERN = Pattern.compile(".*(price|Price|cost|Cost|amount|Amount|fee|Fee|total|Total|payment|salary).*");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(".*(address|Address|street|Street|city|City|state|zip|postal).*");
    private static final Pattern URL_PATTERN = Pattern.compile(".*(url|Url|URL|link|Link|href|website|Website|uri|Uri).*");
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(".*(description|Description|desc|Desc|summary|Summary|about|bio|details).*");
    private static final Pattern STATUS_PATTERN = Pattern.compile(".*(status|Status|state|State|phase|stage).*");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile(".*(is|Is|has|Has|can|Can|should|Should|enabled|Enabled|active|Active|verified).*");
    private static final Pattern COUNT_PATTERN = Pattern.compile(".*(count|Count|quantity|Quantity|number|Number|total|Total|size|Size|length).*");
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile(".*(percent|Percent|rate|Rate|ratio|discount|tax).*");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile(".*(currency|Currency|curr).*");
    private static final Pattern COUNTRY_PATTERN = Pattern.compile(".*(country|Country|nation).*");
    private static final Pattern IMAGE_PATTERN = Pattern.compile(".*(image|Image|photo|Photo|picture|Picture|avatar|logo|icon|thumbnail).*");
    
    public enum FieldType {
        IDENTIFIER,
        EMAIL,
        PHONE,
        PERSON_NAME,
        PRODUCT_NAME,
        DATETIME,
        MONETARY,
        ADDRESS,
        URL,
        DESCRIPTION,
        STATUS,
        BOOLEAN,
        COUNT,
        PERCENTAGE,
        CURRENCY,
        COUNTRY,
        IMAGE_URL,
        GENERIC_STRING,
        GENERIC_NUMBER,
        UNKNOWN
    }

    public String analyzeSchema(String jsonSchema) {
        if (jsonSchema == null || jsonSchema.isBlank()) {
            return "";
        }
        
        try {
            JsonNode schemaNode = objectMapper.readTree(jsonSchema);
            Map<String, FieldType> fieldTypes = new HashMap<>();
            List<String> guidance = new ArrayList<>();
            
            JsonNode properties = schemaNode.get("properties");
            if (properties != null && properties.isObject()) {
                properties.fields().forEachRemaining(entry -> {
                    String fieldName = entry.getKey();
                    JsonNode fieldSchema = entry.getValue();
                    FieldType fieldType = detectFieldType(fieldName, fieldSchema);
                    fieldTypes.put(fieldName, fieldType);
                });
            }
            
            guidance.add("\nFIELD-SPECIFIC SEMANTIC RULES:");
            
            for (Map.Entry<String, FieldType> entry : fieldTypes.entrySet()) {
                String fieldName = entry.getKey();
                FieldType fieldType = entry.getValue();
                String fieldGuidance = generateFieldGuidance(fieldName, fieldType);
                if (!fieldGuidance.isEmpty()) {
                    guidance.add(String.format("- %s: %s", fieldName, fieldGuidance));
                }
            }
            
            addRelationshipRules(fieldTypes, guidance);
            
            if (guidance.size() <= 1) {
                return "";
            }
            
            return String.join("\n", guidance) + "\n";
            
        } catch (Exception e) {
            log.warn("Failed to analyze schema semantics", e);
            return "";
        }
    }

    public FieldType detectFieldType(String fieldName, JsonNode fieldSchema) {
        if (fieldName == null) return FieldType.UNKNOWN;
        
        String type = fieldSchema != null && fieldSchema.has("type") ? 
            fieldSchema.get("type").asText() : "";
        String format = fieldSchema != null && fieldSchema.has("format") ? 
            fieldSchema.get("format").asText() : "";
        
        if ("email".equals(format)) return FieldType.EMAIL;
        if ("date-time".equals(format) || "date".equals(format)) return FieldType.DATETIME;
        if ("uri".equals(format) || "url".equals(format)) return FieldType.URL;
        if ("uuid".equals(format)) return FieldType.IDENTIFIER;
        
        if (ID_PATTERN.matcher(fieldName).matches()) return FieldType.IDENTIFIER;
        if (EMAIL_PATTERN.matcher(fieldName).matches()) return FieldType.EMAIL;
        if (PHONE_PATTERN.matcher(fieldName).matches()) return FieldType.PHONE;
        if (DATE_PATTERN.matcher(fieldName).matches()) return FieldType.DATETIME;
        if (PRICE_PATTERN.matcher(fieldName).matches()) return FieldType.MONETARY;
        if (ADDRESS_PATTERN.matcher(fieldName).matches()) return FieldType.ADDRESS;
        if (URL_PATTERN.matcher(fieldName).matches()) return FieldType.URL;
        if (DESCRIPTION_PATTERN.matcher(fieldName).matches()) return FieldType.DESCRIPTION;
        if (STATUS_PATTERN.matcher(fieldName).matches()) return FieldType.STATUS;
        if (CURRENCY_PATTERN.matcher(fieldName).matches()) return FieldType.CURRENCY;
        if (COUNTRY_PATTERN.matcher(fieldName).matches()) return FieldType.COUNTRY;
        if (IMAGE_PATTERN.matcher(fieldName).matches()) return FieldType.IMAGE_URL;
        
        if ("boolean".equals(type) || BOOLEAN_PATTERN.matcher(fieldName).matches()) {
            return FieldType.BOOLEAN;
        }
        
        if ("integer".equals(type) || "number".equals(type)) {
            if (COUNT_PATTERN.matcher(fieldName).matches()) return FieldType.COUNT;
            if (PERCENTAGE_PATTERN.matcher(fieldName).matches()) return FieldType.PERCENTAGE;
            if (PRICE_PATTERN.matcher(fieldName).matches()) return FieldType.MONETARY;
            return FieldType.GENERIC_NUMBER;
        }
        
        if (NAME_PATTERN.matcher(fieldName).matches()) {
            if (fieldName.toLowerCase().contains("product") || 
                fieldName.toLowerCase().contains("item")) {
                return FieldType.PRODUCT_NAME;
            } else if (fieldName.toLowerCase().contains("user") || 
                       fieldName.toLowerCase().contains("person") ||
                       fieldName.toLowerCase().contains("customer")) {
                return FieldType.PERSON_NAME;
            }
            return FieldType.GENERIC_STRING;
        }
        
        if ("string".equals(type)) return FieldType.GENERIC_STRING;
        
        return FieldType.UNKNOWN;
    }

    private String generateFieldGuidance(String fieldName, FieldType fieldType) {
        return switch (fieldType) {
            case IDENTIFIER -> "Use unique ID like 'usr-78234' or UUID format";
            case EMAIL -> "Use realistic email like 'firstname.lastname@company.com'";
            case PHONE -> "Use E.164 format like '+1-555-0123' or '(555) 234-5678'";
            case PERSON_NAME -> "Use realistic full names from diverse backgrounds";
            case PRODUCT_NAME -> "Use real product names with brand and model";
            case DATETIME -> "Use ISO-8601 format (2024-01-25T14:30:00Z)";
            case MONETARY -> "Use realistic prices with 2 decimal places ($19.99, $1,299.00)";
            case ADDRESS -> "Use real street names with city, state, ZIP";
            case URL -> "Use valid URLs starting with https://";
            case DESCRIPTION -> "Write meaningful, detailed descriptions (50-200 chars)";
            case STATUS -> "Use standard statuses (active, pending, completed, cancelled)";
            case BOOLEAN -> "Use true/false based on realistic probability";
            case COUNT -> "Use realistic quantities (1-100 for most items)";
            case PERCENTAGE -> "Use values 0-100 with decimals (e.g., 87.5)";
            case CURRENCY -> "Use ISO 4217 codes (USD, EUR, GBP, JPY)";
            case COUNTRY -> "Use ISO 3166 country codes or full names";
            case IMAGE_URL -> "Use placeholder image URLs like 'https://via.placeholder.com/300'";
            case GENERIC_STRING -> "Use contextually appropriate text";
            case GENERIC_NUMBER -> "Use realistic numbers for the context";
            case UNKNOWN -> "";
        };
    }

    private void addRelationshipRules(Map<String, FieldType> fieldTypes, List<String> guidance) {
        boolean hasCreatedAt = fieldTypes.containsKey("createdAt") || fieldTypes.containsKey("created");
        boolean hasUpdatedAt = fieldTypes.containsKey("updatedAt") || fieldTypes.containsKey("updated");
        
        if (hasCreatedAt && hasUpdatedAt) {
            guidance.add("- RELATIONSHIP: updatedAt must be >= createdAt");
        }
        
        boolean hasPrice = fieldTypes.values().stream().anyMatch(t -> t == FieldType.MONETARY);
        boolean hasCurrency = fieldTypes.values().stream().anyMatch(t -> t == FieldType.CURRENCY);
        
        if (hasPrice && !hasCurrency) {
            guidance.add("- RELATIONSHIP: Assume USD for prices unless specified");
        }
        
        boolean hasStatus = fieldTypes.values().stream().anyMatch(t -> t == FieldType.STATUS);
        boolean hasActive = fieldTypes.containsKey("active") || fieldTypes.containsKey("isActive");
        
        if (hasStatus && hasActive) {
            guidance.add("- RELATIONSHIP: 'active' status should correlate with isActive=true");
        }
        
        long idCount = fieldTypes.values().stream().filter(t -> t == FieldType.IDENTIFIER).count();
        if (idCount > 1) {
            guidance.add("- RELATIONSHIP: Related IDs should follow consistent format");
        }
        
        boolean hasEmail = fieldTypes.values().stream().anyMatch(t -> t == FieldType.EMAIL);
        boolean hasName = fieldTypes.values().stream().anyMatch(t -> t == FieldType.PERSON_NAME);
        
        if (hasEmail && hasName) {
            guidance.add("- RELATIONSHIP: Email should relate to person's name when possible");
        }
    }
}