# Smart Mock – AI-Powered API Mock Server

An intelligent API mock server that uses **Ollama** and **LangChain4j** to generate realistic, context-aware responses from your OpenAPI specifications.

---

## Features

* **Dynamic Mock Generation** – Automatically create responses matching your OpenAPI spec.
* **Context-Aware Responses** – LLM-powered output that respects schema relationships and semantics.
* **Multiple Scenarios** – Test happy paths, edge cases, validation errors, rate limits, and server errors.
* **Deterministic Mode** – Seed-based generation for reproducible test results.
* **Response Caching** – High-performance caching for faster subsequent calls.
* **Latency Simulation** – Mimic real-world network delays.
* **Schema Compliance** – JSON Schema validation for every generated response.

---

## Quick Start

### Prerequisites

* Java 21+
* Docker & Docker Compose
* Maven

### Run with Docker Compose

1. **Build the application**

   ```bash
   mvn clean package
   ```

2. **Start services**

   ```bash
   docker-compose up -d
   ```

3. **Initialize Ollama models**

   ```bash
   chmod +x init-ollama.sh
   ./init-ollama.sh
   ```

4. **Load an OpenAPI spec**

   ```bash
   curl -X POST http://localhost:8080/admin/spec \
     -H "Content-Type: application/yaml" \
     --data-binary @src/main/resources/sample-petstore.yaml
   ```

5. **Test mock endpoints**

   ```bash
   curl http://localhost:8080/mock/pets
   curl http://localhost:8080/mock/pets/123
   curl -X POST http://localhost:8080/mock/pets \
     -H "Content-Type: application/json" \
     -d '{"name": "Fluffy", "category": "cat"}'
   ```

### Run Locally (without Docker)

1. **Install and start Ollama**

   ```bash
   # macOS
   brew install ollama
   ollama serve

   # Pull required models
   ollama pull codellama:7b
   ollama pull mistral-nemo
   ```

2. **Start Smart Mock**

   ```bash
   mvn spring-boot:run
   ```

---

## Controlling Mock Behaviour

Smart Mock supports special headers to control output.

### Scenarios

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

### Determinism & Variability

```bash
# Fixed seed for reproducible responses
curl -H "X-Mock-Seed: 42" http://localhost:8080/mock/pets

# Control randomness (0.0 = deterministic, 1.0 = max variation)
curl -H "X-Mock-Temperature: 0.5" http://localhost:8080/mock/pets
```

### Latency Simulation

```bash
curl -H "X-Mock-Latency: 250ms" http://localhost:8080/mock/pets
curl -H "X-Mock-Latency: 1.5s" http://localhost:8080/mock/pets
```

### Status Override

```bash
curl -H "X-Mock-Status: 201" http://localhost:8080/mock/pets
```

---

## API Endpoints

### Admin

* `POST /admin/spec` – Upload OpenAPI spec (YAML/JSON)
* `POST /admin/spec/file?filePath=/path/to/spec.yaml` – Load spec from file
* `GET /admin/spec` – Retrieve current spec

### Mock

All OpenAPI-defined paths are available under `/mock/*`.

---

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

---

## Architecture

* **Spring Boot Web** – API handling
* **OpenAPI Parser** – Ingests and indexes specifications
* **LangChain4j + Ollama** – Local LLM response generation
* **Caffeine Cache** – Fast in-memory caching
* **JSON Schema Validator** – Enforces schema compliance

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

---

## Performance

* **Cold start**: 2–5s (first LLM call)
* **Cached responses**: <50ms
* **Throughput**: 100–500 RPS (cached), 5–20 RPS (uncached)
* **Memory usage**: 512MB–2GB (depends on cache size)

---

## Development

**Build from source**

```bash
mvn clean install
```

**Run tests**

```bash
mvn test
```

**Create Docker image**

```bash
mvn clean package
docker build -t smart-mock:latest .
```

---

## Roadmap

* [ ] WebSocket support
* [ ] GraphQL schema support
* [ ] Stateful scenarios
* [ ] Request/response recording
* [ ] UI dashboard
* [ ] Multiple spec support
* [ ] Authentication simulation
* [ ] Custom data generators

---

## License

MIT License

---

## Contributing

Pull requests are welcome. Please see the contributing guidelines before submitting changes.

---

## Support

Use the GitHub issue tracker for questions or bug reports.