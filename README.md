# Smart Mock – AI-Powered API Mock Server

An intelligent API mock server that uses **Ollama** and **LangChain4j** to generate realistic, context-aware responses from your OpenAPI specifications.

---

## Features

* **Dynamic Mock Generation** – Automatically create responses matching your OpenAPI spec.
* **Multi-Schema Support** – Load and switch between multiple API specifications.
* **Persistent Storage** – Schemas are saved to disk and automatically restored on restart.
* **Context-Aware Responses** – LLM-powered output that respects schema relationships and semantics.
* **Multiple Scenarios** – Test happy paths, edge cases, validation errors, rate limits, and server errors.
* **Deterministic Mode** – Seed-based generation for reproducible test results.
* **Response Caching** – High-performance caching for faster subsequent calls.
* **Latency Simulation** – Mimic real-world network delays.
* **Schema Compliance** – JSON Schema validation for every generated response.
* **Web UI** – Upload and explore OpenAPI specs with built-in Swagger UI.
* **Import/Export** – Export schemas to files or import multiple schemas at once.

---

## Quick Start

### Prerequisites

* Java 17+
* Maven 3.6+
* Ollama (for LLM support)

### Run Locally

1. **Install and start Ollama**

   ```bash
   # macOS
   brew install ollama
   ollama serve

   # Linux (using install script)
   curl -fsSL https://ollama.ai/install.sh | sh
   ollama serve

   # Pull required model
   ollama pull codellama:7b
   ```

2. **Start Smart Mock**

   ```bash
   mvn spring-boot:run
   ```

3. **Access the Web UI**

   Open http://localhost:8080 in your browser to:
   - Upload OpenAPI specifications
   - Load sample Pet Store spec
   - Explore APIs with Swagger UI

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

### Schema Management

* `GET /api/schemas` – List all uploaded schemas
* `POST /api/schemas?name={name}` – Upload OpenAPI spec (YAML/JSON)
* `POST /api/schemas/{id}/activate` – Activate a specific schema
* `DELETE /api/schemas/{id}` – Delete a schema
* `POST /api/schemas/load-samples` – Load sample Pet Store schemas
* `GET /api/schemas/{id}/export` – Export a schema to file
* `POST /api/schemas/import` – Import multiple schema files
* `POST /api/schemas/backup` – Manually backup all schemas
* `POST /api/schemas/restore` – Restore schemas from backup

### Mock

All OpenAPI-defined paths are available under `/mock/*`.

### Web UI

* `GET /` – Home page with schema management interface
* `GET /swagger-ui.html` – Swagger UI for API exploration
* `GET /api-spec?schemaId={id}` – OpenAPI spec for Swagger UI (optional schemaId parameter)

---

## Configuration

Configure via environment variables or `application.yml`:

```yaml
ollama:
  base-url: http://localhost:11434
  model-name: codellama:7b
  temperature: 0.2
  timeout: 60

smart-mock:
  storage:
    enabled: true
    path: ${user.home}/.smart-mock/schemas

cache:
  max-size: 1000
  expire-minutes: 15

logging:
  level:
    ca.bazlur.smartmock: INFO
```

---

## Architecture

* **Spring Boot 3.3.2** – REST API and web interface
* **OpenAPI Parser** – Ingests and indexes specifications
* **LangChain4j 1.3.0 + Ollama** – Local LLM response generation
* **Caffeine Cache** – Fast in-memory caching
* **JSON Schema Validator** – Enforces schema compliance
* **Thymeleaf + Bootstrap** – Web UI for spec management
* **SpringDoc OpenAPI** – Swagger UI integration

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

**Package as JAR**

```bash
mvn clean package
java -jar target/smart-mock-*.jar
```

---

## Roadmap

* [x] Web UI for spec upload
* [x] Swagger UI integration
* [ ] WebSocket support
* [ ] GraphQL schema support
* [ ] Stateful scenarios
* [ ] Request/response recording
* [ ] Multiple spec support
* [ ] Authentication simulation
* [ ] Custom data generators
* [ ] Docker deployment optimization

---

## License

MIT License

---

## Contributing

Pull requests are welcome. Please see the contributing guidelines before submitting changes.

---

## Support

Use the GitHub issue tracker for questions or bug reports.