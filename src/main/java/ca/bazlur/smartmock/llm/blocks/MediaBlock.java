package ca.bazlur.smartmock.llm.blocks;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.EndpointInfo;
import org.springframework.stereotype.Component;

@Component
public class MediaBlock implements ContextBlock {
  public String id() {
    return "media.assets.v1";
  }

  public double score(EndpointInfo i) {
    double s = 0.0;
    String p = safe(i.path());
    String op = safe(i.operationId());
    String j = safe(i.jsonSchemaMinified());

    if (p.matches(".*\\b(media|assets?|images?|videos?|tracks?|albums?)\\b.*"))
      s += 0.30;
    if (op.matches(".*(asset|image|video|track|album|media).*")) s += 0.20;
    if (j.matches("(?s).*\\b(mimeType|duration|bitrate|resolution|width|height|artist|title)\\b.*"))
      s += 0.40;
    if (j.matches("(?s).*\\b(url|checksum|size)\\b.*")) s += 0.10;

    return Math.min(1.0, s);
  }

  public String render(EndpointInfo i) {
    return """
        MEDIA CONTEXT:
        - Titles/artists realistic; durations in seconds; resolution WxH; MIME types valid.
        - URLs plausible; include checksums (SHA-256) and byte sizes where relevant.
        """;
  }

  private static String safe(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
