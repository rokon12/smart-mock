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
    return """
        USERS CONTEXT:
        - Use realistic names from diverse backgrounds; no placeholders.
        - Emails RFC 5322, phones E.164, addresses with city/state/ZIP/postal code.
        - Dates ISO-8601; UUIDs for ids where relevant.
        """;
  }

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
