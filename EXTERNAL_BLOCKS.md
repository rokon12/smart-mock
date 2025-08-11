# External Context Blocks

Smart Mock allows you to create custom context blocks to generate domain-specific mock data without modifying the source code.

## Overview

External blocks are YAML or JSON files that define:
- Scoring rules to match endpoints
- Example responses for different scenarios
- Domain-specific rules and guidelines

## Directory Structure

By default, external blocks are loaded from:
```
~/.smart-mock/blocks/
├── banking.yaml
├── inventory.yaml
├── custom-domain.json
└── ...
```

Configure the path in `application.yml`:
```yaml
smart-mock:
  blocks:
    external:
      enabled: true
      path: ${user.home}/.smart-mock/blocks
```

## Block Format

### YAML Example

```yaml
id: banking.accounts.v1
name: Banking
description: Banking and financial accounts

scoring:
  pathPatterns:
    - pattern: "\\b(accounts?|transactions?|balances?)\\b"
      score: 0.35
    - pattern: "\\b(banking|financial)\\b"
      score: 0.20
  operationPatterns:
    - pattern: "(account|transaction|balance)"
      score: 0.25
  schemaPatterns:
    - pattern: "(accountNumber|balance|currency|iban)"
      score: 0.30

examples:
  - name: "bank account"
    condition: "account"  # Optional: when to use this example
    json: |
      {
        "accountId": "ACC-2024-789456",
        "accountNumber": "****4567",
        "type": "checking",
        "balance": 15234.56,
        "currency": "USD",
        "status": "active"
      }
  
  - name: "transaction"
    condition: "transaction"
    json: |
      {
        "transactionId": "TXN-2024-123456",
        "type": "debit",
        "amount": 125.50,
        "description": "Online purchase"
      }

rules:
  - "Use realistic account numbers (masked)"
  - "Include IBAN/SWIFT for international"
  - "Transaction types: debit, credit, transfer"
```

### JSON Example

```json
{
  "id": "inventory.warehouse.v1",
  "name": "Inventory",
  "description": "Warehouse management",
  
  "scoring": {
    "pathPatterns": [
      {
        "pattern": "\\b(inventory|warehouse|stock)\\b",
        "score": 0.35
      }
    ],
    "schemaPatterns": [
      {
        "pattern": "(sku|barcode|quantity)",
        "score": 0.35
      }
    ]
  },
  
  "examples": [
    {
      "name": "inventory item",
      "json": "{\n  \"sku\": \"PRD-ABC-123\",\n  \"quantity\": 145\n}"
    }
  ],
  
  "rules": [
    "Use standard SKU format",
    "Include warehouse locations"
  ]
}
```

## Scoring System

Blocks are scored based on how well they match the endpoint:

- **Path Patterns**: Match against the request path
- **Operation Patterns**: Match against the operation ID
- **Schema Patterns**: Match against the JSON schema fields

Total score is capped at 1.0. Blocks with higher scores are selected.

## Example Conditions

Examples can have conditions to determine when they're used:

- `condition: "account"` - Use when path contains "account"
- `condition: "method:POST"` - Use for POST requests
- `condition: "path:users"` - Use when path contains "users"

## Management API

### List loaded blocks
```bash
curl http://localhost:8080/api/blocks
```

### Reload blocks (after adding/modifying files)
```bash
curl -X POST http://localhost:8080/api/blocks/reload
```

### Check blocks directory
```bash
curl http://localhost:8080/api/blocks/path
```

## Creating Custom Blocks

1. Create a YAML/JSON file in the blocks directory
2. Define scoring rules to match your endpoints
3. Provide realistic examples
4. Add domain-specific rules
5. Reload blocks via API or restart the application

## Best Practices

1. **Specific Scoring**: Make patterns specific enough to match only relevant endpoints
2. **Multiple Examples**: Provide different examples for different scenarios
3. **Clear Rules**: Document data generation rules clearly
4. **Realistic Data**: Use actual sample data from your domain
5. **Version IDs**: Include version in block IDs for updates

## Built-in vs External Blocks

- **Built-in blocks**: Compiled with the application, always available
- **External blocks**: Loaded from files, can be added/modified at runtime
- External blocks can override built-in blocks if they score higher

## Troubleshooting

### Blocks not loading
- Check the logs for errors
- Verify file permissions
- Ensure valid YAML/JSON syntax
- Check pattern escaping (use double backslashes)

### Blocks not matching
- Test patterns with online regex testers
- Check scoring thresholds
- Verify path/operation patterns
- Review logs for scoring details

## Examples Repository

Sample blocks for common domains:
- Banking & Finance
- Healthcare
- E-commerce
- Inventory Management
- IoT & Sensors
- Gaming
- Logistics

Find more examples at: https://github.com/your-org/smart-mock-blocks