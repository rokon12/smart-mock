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
    StringBuilder sb = new StringBuilder();
    sb.append("ORDERS CONTEXT:\n");
    sb.append("Example of excellent order response:\n");
    sb.append(ORDER_EXAMPLE);
    sb.append("\nRules for your response:\n");
    sb.append("- Order IDs like ORD-2024-xxxxx or uuid format\n");
    sb.append("- Include customer reference, items array, shipping, payment\n");
    sb.append("- Status: pending/processing/shipped/delivered/cancelled\n");
    sb.append("- Dates in ISO-8601 format\n");
    sb.append("- Total = sum(items) + shipping + tax\n");
    
    return sb.toString();
  }
  
  private static final String ORDER_EXAMPLE = """
      {
        "orderId": "ORD-2024-78234",
        "customerId": "usr-456789",
        "orderDate": "2024-01-25T14:30:00Z",
        "status": "processing",
        "items": [
          {
            "productId": "prod-789",
            "productName": "Sony WH-1000XM5",
            "quantity": 1,
            "unitPrice": 399.99,
            "subtotal": 399.99
          }
        ],
        "shipping": {
          "method": "Express 2-Day",
          "cost": 12.99
        },
        "totals": {
          "subtotal": 399.99,
          "shipping": 12.99,
          "tax": 35.00,
          "total": 447.98
        }
      }
      """;

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
