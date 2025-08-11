package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class OrdersBlock implements ContextBlock {
  public String id() {
    return "commerce.orders.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(orders?|transactions?|payments?)\\b.*")) s += 0.35;
    if (op.matches(".*(order|checkout|payment|transaction).*")) s += 0.20;
    if (j.matches("(?s).*\\b(status|total|shipping|billing|paymentMethod|lineItems)\\b.*"))
      s += 0.35;
    if (j.matches("(?s).*\\b(currency|amount|tax|discount)\\b.*")) s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    return """
        ORDERS CONTEXT:
        - IDs like "ORD-2025-000123", timestamps ISO-8601, totals with currency.
        - Status: pending, processing, shipped, delivered, cancelled.
        - Shipping: method, carrier, tracking; Payment: masked card/PayPal/Apple Pay.
        """;
  }

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
