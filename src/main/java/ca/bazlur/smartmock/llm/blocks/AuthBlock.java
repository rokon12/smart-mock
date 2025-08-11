package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class AuthBlock implements ContextBlock {
  public String id() {
    return "auth.tokens.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(auth|oauth|login|token|sessions?)\\b.*")) s += 0.35;
    if (op.matches(".*(auth|oauth|token|session|signin|login|refresh).*")) s += 0.25;
    if (j.matches("(?s).*\\b(access_token|refresh_token|expires_in|scope|claims|aud|iss|sub)\\b.*"))
      s += 0.35;
    if (j.matches("(?s).*\\b(jwk|kid|alg)\\b.*")) s += 0.05;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    return """
        AUTH CONTEXT:
        - Use realistic JWTs (header/payload/exp/iat), plausible scopes/claims.
        - Never include real secrets; keys and tokens must be non-sensitive mock values.
        """;
  }

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
