package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class GenericStructuredDataBlock implements ContextBlock {
  public String id() {
    return "generic.structured.v1";
  }

  public double score(EndpointInfo i) {
    return 0.1;
  }

  public String render(EndpointInfo i) {
    return """
        GENERIC CONTEXT:
        - Use realistic, domain-appropriate values; no placeholders.
        - Honor formats/enums (email, uuid, date-time, country codes).
        - Arrays must be diverse; respect min/max constraints in schema when present.
        """;
  }
}
