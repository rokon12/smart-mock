package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.llm.ContextBlock;
import ca.bazlur.smartmock.llm.ContextRegistry;
import ca.bazlur.smartmock.llm.external.ExternalBlockLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(value = "/api/blocks", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BlockManagementController {

  private final ExternalBlockLoader externalBlockLoader;
  private final List<ContextBlock> builtInBlocks;

  @Value("${smart-mock.blocks.external.path:${user.home}/.smart-mock/blocks}")
  private String blocksPath;

  @GetMapping
  public ResponseEntity<Map<String, Object>> listBlocks() {
    Map<String, Object> response = new HashMap<>();

    var internalBlocksList = builtInBlocks.stream()
        .filter(block -> !block.getClass().getName().contains("external"))
        .map(block -> Map.of(
            "id", block.id(),
            "type", "built-in",
            "class", block.getClass().getSimpleName()
        ))
        .collect(Collectors.toList());

    var externalBlocksList = externalBlockLoader.getExternalBlocks().stream()
        .map(block -> Map.of(
            "id", block.id(),
            "type", "external"
        ))
        .collect(Collectors.toList());

    response.put("builtIn", internalBlocksList);
    response.put("external", externalBlocksList);
    response.put("summary", Map.of(
        "builtInCount", internalBlocksList.size(),
        "externalCount", externalBlocksList.size(),
        "totalCount", internalBlocksList.size() + externalBlocksList.size()
    ));
    response.put("externalPath", blocksPath);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/reload")
  public ResponseEntity<Map<String, Object>> reloadBlocks() {
    log.info("Reloading external blocks...");
    externalBlockLoader.reloadBlocks();

    Map<String, Object> response = new HashMap<>();
    response.put("status", "success");
    response.put("message", "External blocks reloaded");
    response.put("count", externalBlockLoader.getExternalBlocks().size());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{blockId}")
  public ResponseEntity<Map<String, Object>> getBlockDetails(@PathVariable String blockId) {
    Map<String, Object> response = new HashMap<>();

    var builtInBlock = builtInBlocks.stream()
        .filter(block -> block.id().equals(blockId))
        .findFirst();

    if (builtInBlock.isPresent()) {
      var block = builtInBlock.get();
      response.put("id", block.id());
      response.put("type", "built-in");
      response.put("class", block.getClass().getSimpleName());

      // Get sample render output
      var sampleInfo = new ca.bazlur.smartmock.llm.EndpointInfo(
          "/sample/path",
          "sampleOperation",
          "GET",
          "{}",
          "{}"
      );
      response.put("sampleOutput", block.render(sampleInfo));

      // Try to extract examples if it's a known block type
      extractBuiltInBlockContent(block, response);

      return ResponseEntity.ok(response);
    }

    var externalBlock = externalBlockLoader.getExternalBlocks().stream()
        .filter(block -> block.id().equals(blockId))
        .findFirst();

    if (externalBlock.isPresent()) {
      var block = externalBlock.get();
      response.put("id", block.id());
      response.put("type", "external");

      // For external blocks, we can access the full definition
      if (block instanceof ca.bazlur.smartmock.llm.external.ExternalContextBlock) {
        var extBlock = (ca.bazlur.smartmock.llm.external.ExternalContextBlock) block;
        var definition = extBlock.getDefinition();

        response.put("name", definition.getName());
        response.put("description", definition.getDescription());
        response.put("scoring", definition.getScoring());
        response.put("examples", definition.getExamples());
        response.put("rules", definition.getRules());
        response.put("metadata", definition.getMetadata());
      }

      return ResponseEntity.ok(response);
    }

    return ResponseEntity.notFound().build();
  }

  private void extractBuiltInBlockContent(ca.bazlur.smartmock.llm.ContextBlock block, Map<String, Object> response) {
    // Use reflection to get static fields containing examples
    try {
      var fields = block.getClass().getDeclaredFields();
      var examples = new HashMap<String, String>();
      var rules = new java.util.ArrayList<String>();

      for (var field : fields) {
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
            field.getType() == String.class &&
            field.getName().contains("EXAMPLE")) {
          field.setAccessible(true);
          var value = (String) field.get(null);
          examples.put(field.getName(), value);
        }
      }

      if (!examples.isEmpty()) {
        response.put("examples", examples);
      }

      // Extract rules from render output
      var sampleInfo = new ca.bazlur.smartmock.llm.EndpointInfo(
          "/products",
          "getProducts",
          "GET",
          "{}",
          "{}"
      );
      var output = block.render(sampleInfo);
      if (output.contains("Rules")) {
        var rulesStart = output.indexOf("Rules");
        if (rulesStart >= 0) {
          var rulesSection = output.substring(rulesStart);
          var lines = rulesSection.split("\n");
          for (var line : lines) {
            if (line.trim().startsWith("-")) {
              rules.add(line.trim().substring(1).trim());
            }
          }
        }
      }

      if (!rules.isEmpty()) {
        response.put("rules", rules);
      }

    } catch (Exception e) {
      log.debug("Could not extract content from built-in block", e);
    }
  }

  @GetMapping("/path")
  public ResponseEntity<Map<String, Object>> getBlocksPath() {
    Map<String, Object> response = new HashMap<>();
    response.put("path", blocksPath);

    try {
      Path path = Paths.get(blocksPath);
      response.put("exists", Files.exists(path));

      if (Files.exists(path)) {
        try (var stream = Files.list(path)) {
          List<String> files = stream.filter(Files::isRegularFile)
              .filter(p -> {
                String name = p.getFileName().toString().toLowerCase();
                return name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json");
              })
              .map(p -> p.getFileName().toString())
              .toList();

          response.put("files", files);
        }
      }
    } catch (Exception e) {
      response.put("error", e.getMessage());
    }

    return ResponseEntity.ok(response);
  }
}