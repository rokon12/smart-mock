package ca.bazlur.smartmock.llm.blocks;


import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class CommerceProductsBlock implements ContextBlock {
  public String id() {
    return "commerce.products.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(products?|items?|catalog|inventory)\\b.*")) s += 0.35;
    if (op.matches(".*(product|catalog|inventory|list).*")) s += 0.20;
    if (j.matches("(?s).*\\b(price|currency|sku|upc|isbn|brand|model|category)\\b.*"))
      s += 0.35;
    if (j.contains("\"type\":\"array\"") && j.contains("\"items\"")) s += 0.05;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    return """
        PRODUCTS CONTEXT:
        - Use real product names and specs (electronics, apparel, books, home).
        - Descriptions must be specific (chipset, materials, capacity, fit, battery life).
        - Prices realistic and varied ($9.99–$1,999.00). No placeholders.
        - If "isbn"/"book" present: include author, publisher, edition, and publication year where applicable.
        """;
  }

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
