# Smart Mock - AI-Powered API Mock Server

An intelligent API mock server that uses Ollama and LangChain4j to generate realistic, context-aware responses based on OpenAPI specifications.

## Features

- **Dynamic Mock Generation**: Automatically generate responses matching your OpenAPI spec
- **Intelligent Responses**: LLM-powered responses that understand context and relationships
- **Multiple Scenarios**: Support for happy path, edge cases, errors, and rate limiting
- **Deterministic Control**: Seed-based generation for reproducible responses
- **Response Caching**: Fast responses with intelligent caching
- **Latency Simulation**: Simulate real-world network conditions
- **JSON Schema Validation**: Ensure all responses conform to your API spec

## Quick Start

### Prerequisites

- Java 21+
- Docker and Docker Compose
- Maven

### Running with Docker Compose

1. Build the application:
```bash
mvn clean package
```

2. Start the services:
```bash
docker-compose up -d
```

3. Initialize Ollama models:
```bash
chmod +x init-ollama.sh
./init-ollama.sh
```

4. Load an OpenAPI spec:
```bash
curl -X POST http://localhost:8080/admin/spec \
  -H "Content-Type: application/yaml" \
  --data-binary @src/main/resources/sample-petstore.yaml
```

5. Test the mock endpoints:
```bash
# List pets
curl http://localhost:8080/mock/pets

# Get specific pet
curl http://localhost:8080/mock/pets/123

# Create a pet
curl -X POST http://localhost:8080/mock/pets \
  -H "Content-Type: application/json" \
  -d '{"name": "Fluffy", "category": "cat"}'
```

### Running Locally

1. Install and start Ollama:
```bash
# macOS
brew install ollama
ollama serve

# Pull required models
ollama pull llama3.1:8b
ollama pull mistral-nemo
```

2. Run the application:
```bash
mvn spring-boot:run
```

## Control Headers

Control mock behavior using special headers:

### Scenario Control
```bash
# Happy path (default)
curl -H "X-Mock-Scenario: happy" http://localhost:8080/mock/pets

# Edge cases
curl -H "X-Mock-Scenario: edge" http://localhost:8080/mock/pets

# Validation errors
curl -H "X-Mock-Scenario: invalid" http://localhost:8080/mock/pets

# Rate limiting
curl -H "X-Mock-Scenario: rate-limit" http://localhost:8080/mock/pets

# Server errors
curl -H "X-Mock-Scenario: server-error" http://localhost:8080/mock/pets
```

### Determinism and Variability
```bash
# Fixed seed for reproducible responses
curl -H "X-Mock-Seed: 42" http://localhost:8080/mock/pets

# Control randomness (0.0 = deterministic, 1.0 = maximum variation)
curl -H "X-Mock-Temperature: 0.5" http://localhost:8080/mock/pets
```

### Latency Simulation
```bash
# Add 250ms latency
curl -H "X-Mock-Latency: 250ms" http://localhost:8080/mock/pets

# Add 1.5 second latency
curl -H "X-Mock-Latency: 1.5s" http://localhost:8080/mock/pets
```

### Status Override
```bash
# Force specific status code
curl -H "X-Mock-Status: 201" http://localhost:8080/mock/pets
```

## API Endpoints

### Admin Endpoints

- `POST /admin/spec` - Upload OpenAPI specification (YAML/JSON)
- `POST /admin/spec/file?filePath=/path/to/spec.yaml` - Load spec from file
- `GET /admin/spec` - Get current loaded specification

### Mock Endpoints

All paths defined in your OpenAPI spec are available under `/mock/*`

## Configuration

Configure via environment variables or `application.yml`:

```yaml
ollama:
  base-url: http://localhost:11434
  model-name: llama3.1:8b
  temperature: 0.2
  timeout: 60

cache:
  max-size: 1000
  expire-minutes: 15

logging:
  level:
    ca.bazlur.smartmock: INFO
```

## Architecture

- **Spring Boot Web**: Traditional servlet-based HTTP handling
- **OpenAPI Parser**: Swagger parser for spec ingestion
- **LangChain4j + Ollama**: Local LLM for response generation
- **Caffeine Cache**: High-performance response caching
- **JSON Schema Validator**: NetworkNT validator for schema compliance

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Client    │────▶│ MockController│────▶│ MockService │
└─────────────┘     └──────────────┘     └─────────────┘
                                               │
                           ┌───────────────────┼───────────────────┐
                           │                   │                   │
                     ┌─────▼─────┐     ┌──────▼──────┐    ┌──────▼──────┐
                     │OpenAPI    │     │Response     │    │  LLM Runner │
                     │Index      │     │Planner      │    │  (Ollama)   │
                     └───────────┘     └─────────────┘    └─────────────┘
                                               │
                                        ┌──────▼──────┐
                                        │JSON Schema  │
                                        │Validator    │
                                        └─────────────┘
```

## Performance

- **Cold start**: 2-5 seconds (first LLM generation)
- **Cached responses**: <50ms
- **Throughput**: 100-500 RPS (cached), 5-20 RPS (uncached)
- **Memory**: 512MB-2GB depending on cache size

## Development

### Building from Source
```bash
mvn clean install
```

### Running Tests
```bash
mvn test
```

### Creating a Docker Image
```bash
mvn clean package
docker build -t smart-mock:latest .
```

## Roadmap

- [ ] WebSocket support
- [ ] GraphQL schema support
- [ ] Stateful mock scenarios
- [ ] Request/response recording
- [ ] UI dashboard
- [ ] Multi-spec support
- [ ] Authentication simulation
- [ ] Custom data generators

## License

MIT

## Contributing

Pull requests are welcome! Please read our contributing guidelines first.

## Support

For issues and questions, please use the GitHub issue tracker.