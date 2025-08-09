package ca.bazlur.smartmock.config;

import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.ollama.OllamaLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Slf4j
@Configuration
public class LangChainConfig {

  @Value("${ollama.base-url:http://localhost:11434}")
  private String ollamaBaseUrl;

  @Value("${ollama.model-name:llama3.1:8b}")
  private String modelName;

  @Value("${ollama.temperature:0.2}")
  private Double temperature;

  @Value("${ollama.timeout:60}")
  private Integer timeoutSeconds;

  @Bean
  @Primary
  public LanguageModel chatLanguageModel() {
    log.info("Configuring Ollama language model: {} at {}", modelName, ollamaBaseUrl);

    return OllamaLanguageModel.builder()
        .baseUrl(ollamaBaseUrl)
        .modelName(modelName)
        .temperature(temperature)
        .timeout(Duration.ofSeconds(timeoutSeconds))
        .build();
  }

  @Bean
  public LanguageModel fallbackLanguageModel() {
    log.info("Configuring fallback Ollama model");

    return OllamaLanguageModel.builder()
        .baseUrl(ollamaBaseUrl)
        .modelName("mistral-nemo")
        .temperature(0.1)
        .timeout(Duration.ofSeconds(timeoutSeconds))
        .build();
  }
}