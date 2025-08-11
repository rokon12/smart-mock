package ca.bazlur.smartmock.llm;

import ca.bazlur.smartmock.llm.external.ExternalBlockLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class ContextRegistry {

  private final List<ContextBlock> allBlocks;
  
  public ContextRegistry(List<ContextBlock> builtInBlocks, ExternalBlockLoader externalLoader) {
    this.allBlocks = new ArrayList<>();
    this.allBlocks.addAll(builtInBlocks);
    if (externalLoader != null) {
      this.allBlocks.addAll(externalLoader.getExternalBlocks());
    }
    log.info("ContextRegistry initialized with {} total blocks ({} built-in, {} external)", 
             allBlocks.size(), 
             builtInBlocks.size(), 
             externalLoader != null ? externalLoader.getExternalBlocks().size() : 0);
  }

  public List<ContextBlock> select(EndpointInfo info, int maxBlocks, double minScore, int budgetChars) {
    var ranked = allBlocks.stream()
        .map(b -> new Scored<>(b, safeScore(b, info)))
        .filter(s -> s.score >= minScore)
        .sorted(Comparator.comparingDouble((Scored<ContextBlock> s) -> s.score).reversed())
        .toList();

    String picks = ranked.stream().limit(5)
        .map(s -> s.value.id() + ":" + String.format("%.2f", s.score)).reduce((a, b) -> a + ", " + b).orElse("-");
    log.debug("ContextRegistry candidates: {}", picks);

    StringBuilder budgetProbe = new StringBuilder();
    var chosen = new java.util.ArrayList<ContextBlock>();
    int remaining = Math.max(500, budgetChars);

    for (Scored<ContextBlock> s : ranked) {
      String sample = s.value.render(info);
      int len = sample.length();
      if (len <= remaining) {
        chosen.add(s.value);
        remaining -= len;
      }
      if (chosen.size() >= maxBlocks) break;
    }

    if (chosen.isEmpty()) {
      allBlocks.stream()
          .filter(b -> b.id().startsWith("generic.structured"))
          .findFirst()
          .ifPresent(chosen::add);
    }
    return chosen;
  }

  private static double safeScore(ContextBlock b, EndpointInfo info) {
    try {
      double s = b.score(info);
      if (Double.isNaN(s) || Double.isInfinite(s)) return 0.0;
      return Math.min(1.0, Math.max(0.0, s));
    } catch (Exception e) {
      return 0.0;
    }
  }

  private record Scored<T>(T value, double score) {
  }
}
