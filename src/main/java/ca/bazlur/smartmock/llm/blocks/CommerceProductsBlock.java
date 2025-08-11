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
    String method = i.method().toUpperCase();
    String path = i.path().toLowerCase();

    boolean isSingle = path.matches(".*/(\\{[^}]+\\}|:\\w+|\\d+).*");
    boolean isCreate = method.equals("POST");

    StringBuilder sb = new StringBuilder();
    sb.append("PRODUCTS CONTEXT:\n");

    if (isSingle && !isCreate) {
      sb.append("Example of excellent single product response:\n");
      sb.append(SINGLE_PRODUCT_EXAMPLE);
    } else if (isCreate) {
      sb.append("Example of product creation response:\n");
      sb.append(CREATED_PRODUCT_EXAMPLE);
    } else {
      sb.append("Example of excellent product list:\n");
      sb.append(PRODUCT_LIST_EXAMPLE);
    }

    sb.append("\nRules for your response:\n");
    sb.append("- Use real product names and specs like the example\n");
    sb.append("- Descriptions must be specific (chipset, materials, capacity)\n");
    sb.append("- Prices realistic and varied ($9.99–$1,999.00)\n");
    sb.append("- Each product must be unique - no duplicates or placeholders\n");

    return sb.toString();
  }

  private static final String SINGLE_PRODUCT_EXAMPLE = """
      {
        "id": "prod-789456",
        "name": "Sony WH-1000XM5 Wireless Headphones",
        "description": "Industry-leading noise canceling with Auto NC Optimizer, 30-hour battery life",
        "price": 399.99,
        "category": "Electronics",
        "brand": "Sony",
        "sku": "SNY-WH1000XM5-BLK",
        "inStock": true,
        "stockQuantity": 145,
        "rating": 4.7
      }
      """;

  private static final String PRODUCT_LIST_EXAMPLE = """
      [
        {
          "id": "prod-001",
          "name": "MacBook Air 15-inch M2",
          "price": 1299.00,
          "category": "Computers",
          "brand": "Apple"
        },
        {
          "id": "prod-002", 
          "name": "Samsung Galaxy S24 Ultra",
          "price": 1199.99,
          "category": "Smartphones",
          "brand": "Samsung"
        }
      ]
      """;

  private static final String CREATED_PRODUCT_EXAMPLE = """
      {
        "id": "prod-new-8934",
        "name": "iPad Pro 12.9-inch M2",
        "status": "created",
        "message": "Product created successfully",
        "createdAt": "2024-01-25T09:15:30Z"
      }
      """;

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
