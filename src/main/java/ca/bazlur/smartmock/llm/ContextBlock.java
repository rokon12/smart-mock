package ca.bazlur.smartmock.llm;

public interface ContextBlock {
  String id();
  double score(EndpointInfo info);
  String render(EndpointInfo info);
}