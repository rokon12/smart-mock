package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class PeopleBlock implements ContextBlock {
  public String id() {
    return "people.users.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(users?|customers?|people|persons?)\\b.*")) s += 0.35;
    if (op.matches(".*(user|customer|person|account).*")) s += 0.20;
    if (j.matches("(?s).*\\b(email|firstName|lastName|phone|address|dob|profile)\\b.*"))
      s += 0.35;
    if (j.matches("(?s).*\\b(country|postalCode|zip|city|state|province)\\b.*"))
      s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    String path = i.path().toLowerCase();
    boolean isSingle = path.matches(".*/(\\{[^}]+\\}|:\\w+|\\d+).*");
    
    StringBuilder sb = new StringBuilder();
    sb.append("USERS CONTEXT:\n");
    
    if (isSingle) {
      sb.append("Example of excellent user response:\n");
      sb.append(SINGLE_USER_EXAMPLE);
    } else {
      sb.append("Example of excellent user list:\n");
      sb.append(USER_LIST_EXAMPLE);
    }
    
    sb.append("\nRules for your response:\n");
    sb.append("- Use realistic names from diverse backgrounds\n");
    sb.append("- Emails must be valid (firstname.lastname@domain.com)\n");
    sb.append("- Phone numbers in proper format (+1-555-0123)\n");
    sb.append("- Dates in ISO-8601 format\n");
    
    return sb.toString();
  }
  
  private static final String SINGLE_USER_EXAMPLE = """
      {
        "id": "usr-456789",
        "email": "sarah.johnson@techcorp.com",
        "firstName": "Sarah",
        "lastName": "Johnson",
        "phoneNumber": "+1-415-555-0142",
        "accountStatus": "active",
        "createdAt": "2022-01-10T08:00:00Z"
      }
      """;
  
  private static final String USER_LIST_EXAMPLE = """
      [
        {
          "id": "usr-001",
          "email": "john.smith@example.com",
          "name": "John Smith",
          "role": "customer",
          "status": "active"
        },
        {
          "id": "usr-002",
          "email": "maria.garcia@example.com",
          "name": "Maria Garcia",
          "role": "premium",
          "status": "active"
        }
      ]
      """;

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
