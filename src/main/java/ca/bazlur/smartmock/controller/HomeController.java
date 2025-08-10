package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.config.AppProperties;
import ca.bazlur.smartmock.service.SchemaManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final SchemaManager schemaManager;
    private final AppProperties appProperties;
    
    @Value("${ollama.model-name:codellama:7b}")
    private String ollamaModel;

    @GetMapping("/")
    public String home(Model model) {
        var schemas = schemaManager.getAllSchemas();
        var activeSchema = schemaManager.getActiveSchema().orElse(null);
        
        model.addAttribute("schemas", schemas);
        model.addAttribute("activeSchema", activeSchema);
        model.addAttribute("specLoaded", activeSchema != null);
        
        if (activeSchema != null) {
            var openApi = activeSchema.getIndex().getOpenAPI();
            var info = openApi.getInfo();
            
            model.addAttribute("specTitle", info != null ? info.getTitle() : "Untitled API");
            model.addAttribute("specVersion", info != null ? info.getVersion() : null);
            model.addAttribute("specDescription", info != null ? info.getDescription() : null);
            
            int endpointCount = 0;
            if (openApi.getPaths() != null) {
                for (var path : openApi.getPaths().values()) {
                    if (path.getGet() != null) endpointCount++;
                    if (path.getPost() != null) endpointCount++;
                    if (path.getPut() != null) endpointCount++;
                    if (path.getDelete() != null) endpointCount++;
                    if (path.getPatch() != null) endpointCount++;
                }
            }
            model.addAttribute("endpointCount", endpointCount);
        }
        
        Map<String, String> techStack = new LinkedHashMap<>();
        var deps = appProperties.getDependencies();
        
        techStack.put("Spring Boot " + deps.getSpringBoot(), "Modern Java web framework");
        techStack.put("LangChain4j " + deps.getLangchain4j(), "LLM integration framework");
        techStack.put("Ollama (" + ollamaModel + ")", "Local LLM runtime");
        techStack.put("Caffeine " + deps.getCaffeine(), "High-performance caching library");
        techStack.put("Swagger Parser " + deps.getSwaggerParser(), "OpenAPI specification processing");
        techStack.put("JSON Schema Validator " + deps.getJsonSchemaValidator(), "Response compliance validation");
        techStack.put("Java " + appProperties.getJava().getVersion(), "Runtime environment");
        
        model.addAttribute("techStack", techStack);
        model.addAttribute("projectVersion", appProperties.getVersion());
        model.addAttribute("osInfo", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        
        return "index";
    }
}