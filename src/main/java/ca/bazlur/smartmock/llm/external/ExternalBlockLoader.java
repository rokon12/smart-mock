package ca.bazlur.smartmock.llm.external;

import ca.bazlur.smartmock.llm.ContextBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class ExternalBlockLoader {
    
    private final YAMLMapper yamlMapper;
    private final ObjectMapper jsonMapper;
    
    public ExternalBlockLoader(@Qualifier("yamlMapper") YAMLMapper yamlMapper,
                               @Qualifier("objectMapper") ObjectMapper jsonMapper) {
        this.yamlMapper = yamlMapper;
        this.jsonMapper = jsonMapper;
    }
    
    @Value("${smart-mock.blocks.external.enabled:true}")
    private boolean enabled;
    
    @Value("${smart-mock.blocks.external.path:${user.home}/.smart-mock/blocks}")
    private String blocksPath;
    
    private final List<ContextBlock> externalBlocks = new ArrayList<>();
    
    @PostConstruct
    public void loadExternalBlocks() {
        if (!enabled) {
            log.info("External blocks loading is disabled");
            return;
        }
        
        try {
            Path blockDir = Paths.get(blocksPath);
            
            if (!Files.exists(blockDir)) {
                Files.createDirectories(blockDir);
                log.info("Created external blocks directory: {}", blockDir);
                createSampleBlocks(blockDir);
            }
            
            loadBlocksFromDirectory(blockDir);
            
        } catch (Exception e) {
            log.error("Failed to load external blocks from: {}", blocksPath, e);
        }
    }
    
    private void loadBlocksFromDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory, 1)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> {
                     String name = p.getFileName().toString().toLowerCase();
                     return name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json");
                 })
                 .forEach(this::loadBlockFile);
                 
            log.info("Loaded {} external context blocks from {}", externalBlocks.size(), directory);
            
        } catch (Exception e) {
            log.error("Error scanning blocks directory: {}", directory, e);
        }
    }
    
    private void loadBlockFile(Path file) {
        try {
            String content = Files.readString(file);
            String fileName = file.getFileName().toString().toLowerCase();
            
            ExternalBlockDefinition definition;
            if (fileName.endsWith(".json")) {
                definition = jsonMapper.readValue(content, ExternalBlockDefinition.class);
            } else {
                definition = yamlMapper.readValue(content, ExternalBlockDefinition.class);
            }
            
            if (definition.getId() == null) {
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                definition.setId("external." + baseName);
            }
            
            ExternalContextBlock block = new ExternalContextBlock(definition);
            externalBlocks.add(block);
            
            log.info("Loaded external block '{}' from {}", definition.getId(), file.getFileName());
            
        } catch (Exception e) {
            log.error("Failed to load block from file: {}", file, e);
        }
    }
    
    public List<ContextBlock> getExternalBlocks() {
        return new ArrayList<>(externalBlocks);
    }
    
    public void reloadBlocks() {
        externalBlocks.clear();
        loadExternalBlocks();
    }
    
    private void createSampleBlocks(Path directory) {
        try {
            String sampleBanking = """
                id: banking.accounts.v1
                name: Banking
                description: Banking and financial accounts
                
                scoring:
                  pathPatterns:
                    - pattern: "\\\\b(accounts?|transactions?|balances?|transfers?)\\\\b"
                      score: 0.35
                    - pattern: "\\\\b(banking|financial|money)\\\\b"
                      score: 0.20
                  operationPatterns:
                    - pattern: "(account|transaction|balance|transfer)"
                      score: 0.25
                  schemaPatterns:
                    - pattern: "(accountNumber|balance|currency|iban|swift)"
                      score: 0.30
                
                examples:
                  - name: "bank account"
                    condition: "account"
                    json: |
                      {
                        "accountId": "ACC-2024-789456",
                        "accountNumber": "****4567",
                        "type": "checking",
                        "balance": 15234.56,
                        "currency": "USD",
                        "status": "active",
                        "holder": "John Smith",
                        "iban": "US12 3456 7890 1234 5678 90",
                        "createdAt": "2020-05-15T10:00:00Z"
                      }
                  
                  - name: "transaction"
                    condition: "transaction"
                    json: |
                      {
                        "transactionId": "TXN-2024-123456",
                        "accountId": "ACC-2024-789456",
                        "type": "debit",
                        "amount": 125.50,
                        "currency": "USD",
                        "description": "Online purchase - Amazon",
                        "merchant": "Amazon.com",
                        "category": "Shopping",
                        "status": "completed",
                        "timestamp": "2024-01-25T14:30:00Z"
                      }
                
                rules:
                  - "Use realistic account numbers (masked with asterisks)"
                  - "Include IBAN/SWIFT codes for international accounts"
                  - "Balance should be realistic (positive or negative with overdraft)"
                  - "Transaction types: debit, credit, transfer, fee"
                  - "Include merchant category codes where applicable"
                """;
            
            Path bankingFile = directory.resolve("banking.yaml");
            Files.writeString(bankingFile, sampleBanking);
            log.info("Created sample block file: {}", bankingFile);
            
            String sampleInventory = """
                id: inventory.warehouse.v1
                name: Inventory
                description: Warehouse and inventory management
                
                scoring:
                  pathPatterns:
                    - pattern: "\\\\b(inventory|warehouse|stock|supplies?)\\\\b"
                      score: 0.35
                    - pattern: "\\\\b(items?|products?|skus?)\\\\b"
                      score: 0.15
                  schemaPatterns:
                    - pattern: "(sku|barcode|quantity|warehouse|location)"
                      score: 0.35
                
                examples:
                  - name: "inventory item"
                    json: |
                      {
                        "itemId": "INV-2024-5678",
                        "sku": "PRD-ABC-123",
                        "barcode": "793573024589",
                        "name": "Wireless Mouse - Logitech MX Master 3",
                        "category": "Electronics",
                        "quantity": 145,
                        "reorderPoint": 20,
                        "reorderQuantity": 100,
                        "location": {
                          "warehouse": "WH-01",
                          "zone": "A",
                          "shelf": "A-15-3"
                        },
                        "unitCost": 45.50,
                        "lastRestocked": "2024-01-10T08:00:00Z",
                        "supplier": "Tech Distributors Inc"
                      }
                
                rules:
                  - "Use standard SKU format (e.g., PRD-XXX-###)"
                  - "Include warehouse location details (zone, shelf, bin)"
                  - "Quantity should be realistic based on item type"
                  - "Include reorder points and quantities"
                  - "Add supplier information and lead times"
                """;
            
            Path inventoryFile = directory.resolve("inventory.yaml");
            Files.writeString(inventoryFile, sampleInventory);
            log.info("Created sample block file: {}", inventoryFile);
            
        } catch (Exception e) {
            log.error("Failed to create sample blocks", e);
        }
    }
}