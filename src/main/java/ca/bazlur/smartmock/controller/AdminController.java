package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.openapi.OpenApiIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final OpenApiIndex openApiIndex;

    @PostMapping("/spec")
    public ResponseEntity<String> uploadSpec(@RequestBody String specContent) {
        openApiIndex.loadSpec(specContent);
        return ResponseEntity.ok("OpenAPI spec loaded successfully");
    }

    @PostMapping("/spec/file")
    public ResponseEntity<String> uploadSpecFile(@RequestParam String filePath) {
        openApiIndex.loadSpecFromFile(filePath);
        return ResponseEntity.ok("OpenAPI spec loaded from file successfully");
    }

    @GetMapping("/spec")
    public ResponseEntity<Object> getSpec() {
        if (openApiIndex.getOpenAPI() != null) {
            return ResponseEntity.ok(openApiIndex.getOpenAPI());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}