package ca.bazlur.smartmock.model;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

@Getter
public enum Scenario {
  HAPPY("happy"),
  EDGE("edge"),
  INVALID("invalid"),
  RATE_LIMIT("rate-limit"),
  SERVER_ERROR("server-error");

  private final String value;

  Scenario(String value) {
    this.value = value;
  }

  public static Scenario fromHeaders(HttpServletRequest request) {
    String scenarioHeader = request.getHeader("X-Mock-Scenario");
    return fromString(scenarioHeader);
  }

  public static Scenario fromString(String value) {
    if (value != null) {
      for (Scenario scenario : values()) {
        if (scenario.value.equalsIgnoreCase(value)) {
          return scenario;
        }
      }
    }
    return HAPPY;
  }
}