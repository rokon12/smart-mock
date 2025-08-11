package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class GeoBlock implements ContextBlock {
  public String id() {
    return "geo.locations.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(geo|locations?|addresses?|places?)\\b.*")) s += 0.30;
    if (op.matches(".*(location|address|place|geocode|map).*")) s += 0.20;
    if (j.matches("(?s).*\\b(latitude|longitude|lat|lng|country|postalCode|timezone)\\b.*"))
      s += 0.40;
    if (j.matches("(?s).*\\b(bounds|radius|distance)\\b.*")) s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    return """
        GEO CONTEXT:
        - Coordinates within valid ranges; country codes ISO 3166-1 alpha-2; timezones IANA.
        - Addresses realistic with street/city/state/postal code; distances with units (km/mi).
        """;
  }

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
