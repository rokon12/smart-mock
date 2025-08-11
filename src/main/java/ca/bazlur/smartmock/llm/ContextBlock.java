package ca.bazlur.smartmock.llm;

public interface ContextBlock {
  String id();
  double score(EndpointInfo info);   // 0..1
  String render(EndpointInfo info);  // short, domain guidance
}