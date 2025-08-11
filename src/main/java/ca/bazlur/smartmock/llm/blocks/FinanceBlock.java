package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class FinanceBlock implements ContextBlock {
  public String id() {
    return "finance.accounts.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(accounts?|ledgers?|balances?|bank|finance|payments?)\\b.*"))
      s += 0.30;
    if (op.matches(".*(account|balance|ledger|payment|payout|invoice).*")) s += 0.20;
    if (j.matches("(?s).*\\b(iban|bic|swift|routing|accountNumber|currency|amount)\\b.*"))
      s += 0.40;
    if (j.matches("(?s).*\\b(statement|transactionDate)\\b.*")) s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    return """
        FINANCE CONTEXT:
        - Use valid ISO 4217 currency codes; amounts with 2 decimals where applicable.
        - Bank fields realistic (IBAN/BIC/SWIFT formats); mask sensitive numbers.
        - Dates and value dates in ISO-8601 with time zones.
        """;
  }

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
