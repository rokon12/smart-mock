# Smart Mock Server - Architecture Diagrams

## High-Level Concept

```mermaid
flowchart LR
    Spec[📄 OpenAPI<br/>Spec] --> Mock[🤖 Smart Mock<br/>Server]
    Mock <--> LLM[🧠 Local LLM<br/>Ollama]
    Client[💻 Your App] --> Mock
    Mock --> JSON[📦 Intelligent<br/>JSON Response]
    JSON --> Client
    
    style Spec fill:#fff9c4
    style Mock fill:#f3e5f5
    style LLM fill:#fce4ec
    style Client fill:#e1f5fe
    style JSON fill:#e8f5e9
```

## Simple Flow Diagram

```mermaid
graph TB
    Upload[Upload OpenAPI Spec]
    Server[Smart Mock Server]
    LLM[Local LLM<br/>Ollama]
    Response[Intelligent<br/>Mock Response]
    
    Upload -->|"Define API"| Server
    Server -->|"Generate Request"| LLM
    LLM -->|"AI-Generated Data"| Server
    Server -->|"Schema-Compliant JSON"| Response
    
    Client[API Client] -->|"HTTP Request"| Server
    Response -->|"HTTP Response"| Client

    style Upload fill:#e1f5fe
    style Server fill:#f3e5f5
    style LLM fill:#fce4ec
    style Response fill:#e8f5e9
    style Client fill:#fff3e0
```

## Detailed Architecture Overview

```mermaid
graph TB
    subgraph "Client Layer"
        Browser[Web Browser]
        SwaggerUI[Swagger UI]
        APIClient[API Client]
    end

    subgraph "Smart Mock Server"
        subgraph "Web Layer"
            HomeController[Home Controller<br/>Upload & UI]
            MockController[Mock Controller<br/>Dynamic Endpoints]
            SchemaController[Schema Controller<br/>Multi-Schema Management]
            ApiExplorer[API Explorer<br/>Swagger Integration]
        end

        subgraph "Service Layer"
            MockService[Mock Service<br/>Response Orchestration]
            ResponsePlanner[Response Planner<br/>Prompt Engineering]
            ScenarioEngine[Scenario Engine<br/>Context Management]
            ValidationService[Validation Service<br/>Schema Compliance]
        end

        subgraph "Data Layer"
            SchemaManager[Schema Manager<br/>Multi-Schema Registry]
            ResponseCache[Response Cache<br/>Caffeine]
            SpecStorage[Spec Storage<br/>In-Memory]
        end

        subgraph "AI Layer"
            LangChain[LangChain4j<br/>LLM Integration]
            PromptBuilder[Prompt Builder<br/>Template Engine]
        end
    end

    subgraph "External Services"
        Ollama[Ollama<br/>Local LLM Server]
        LLModel[Language Model<br/>CodeLlama/Mistral]
    end

    Browser --> HomeController
    SwaggerUI --> ApiExplorer
    APIClient --> MockController
    
    HomeController --> SchemaManager
    MockController --> MockService
    SchemaController --> SchemaManager
    ApiExplorer --> SchemaManager
    
    MockService --> ResponsePlanner
    MockService --> ScenarioEngine
    MockService --> ResponseCache
    MockService --> ValidationService
    
    ResponsePlanner --> PromptBuilder
    ResponsePlanner --> LangChain
    
    LangChain --> Ollama
    Ollama --> LLModel
    
    SchemaManager --> SpecStorage
    ValidationService --> SchemaManager

    classDef client fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef web fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef service fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef data fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    classDef ai fill:#fce4ec,stroke:#880e4f,stroke-width:2px
    classDef external fill:#f5f5f5,stroke:#424242,stroke-width:2px

    class Browser,SwaggerUI,APIClient client
    class HomeController,MockController,SchemaController,ApiExplorer web
    class MockService,ResponsePlanner,ScenarioEngine,ValidationService service
    class SchemaManager,ResponseCache,SpecStorage data
    class LangChain,PromptBuilder ai
    class Ollama,LLModel external
```

## Request Flow Sequence

```mermaid
sequenceDiagram
    participant Client
    participant MockController
    participant SchemaManager
    participant MockService
    participant ResponseCache
    participant ResponsePlanner
    participant LangChain4j
    participant Ollama

    Client->>MockController: GET /mock/api/pets
    MockController->>SchemaManager: getActiveIndex()
    SchemaManager-->>MockController: OpenApiIndex
    
    MockController->>MockService: generate(endpoint, headers)
    
    MockService->>ResponseCache: check cache(key)
    alt Cache Hit
        ResponseCache-->>MockService: Cached response
    else Cache Miss
        MockService->>ResponsePlanner: plan(endpoint, scenario)
        ResponsePlanner->>ResponsePlanner: Build prompt
        ResponsePlanner->>LangChain4j: generate(prompt)
        LangChain4j->>Ollama: LLM inference
        Ollama-->>LangChain4j: Generated JSON
        LangChain4j-->>ResponsePlanner: Raw response
        
        ResponsePlanner->>ResponsePlanner: Validate JSON Schema
        alt Invalid Response
            loop Repair Attempts
                ResponsePlanner->>LangChain4j: repair(errors)
                LangChain4j->>Ollama: Fix JSON
                Ollama-->>LangChain4j: Repaired JSON
                LangChain4j-->>ResponsePlanner: Fixed response
                ResponsePlanner->>ResponsePlanner: Re-validate
            end
        end
        
        ResponsePlanner-->>MockService: Valid response
        MockService->>ResponseCache: store(key, response)
    end
    
    MockService-->>MockController: Response + headers
    MockController-->>Client: HTTP Response
```

## Component Details

```mermaid
graph LR
    subgraph "Response Generation Pipeline"
        Extract[Extract Schema<br/>from OpenAPI]
        Analyze[Analyze<br/>Scenario]
        Build[Build<br/>Prompt]
        Generate[Generate<br/>via LLM]
        Validate[Validate<br/>JSON Schema]
        Repair[Repair<br/>if Invalid]
        Cache[Cache<br/>Response]
        
        Extract --> Analyze
        Analyze --> Build
        Build --> Generate
        Generate --> Validate
        Validate -->|Invalid| Repair
        Repair --> Validate
        Validate -->|Valid| Cache
    end

    style Extract fill:#e8eaf6
    style Analyze fill:#e8eaf6
    style Build fill:#e8eaf6
    style Generate fill:#ffebee
    style Validate fill:#e8f5e9
    style Repair fill:#fff3e0
    style Cache fill:#f3e5f5
```

## Scenario Engine State Machine

```mermaid
stateDiagram-v2
    [*] --> DefaultState
    DefaultState --> Happy: header=happy
    DefaultState --> Edge: header=edge  
    DefaultState --> Invalid: header=invalid
    DefaultState --> RateLimit: header=rate-limit
    DefaultState --> ServerError: header=server-error
    
    Happy --> GenerateSuccess: Generate full data
    Edge --> GenerateMinimal: Generate edge cases
    Invalid --> Generate400: Generate validation errors
    RateLimit --> Generate429: Generate rate limit response
    ServerError --> Generate500: Generate server error
    
    GenerateSuccess --> [*]
    GenerateMinimal --> [*]
    Generate400 --> [*]
    Generate429 --> [*]
    Generate500 --> [*]
```

## Cache Strategy

```mermaid
graph TD
    Request[Incoming Request]
    
    Request --> KeyGen{Generate Cache Key}
    KeyGen --> CheckCache{Cache Lookup}
    
    CheckCache -->|Hit| ReturnCached[Return Cached Response]
    CheckCache -->|Miss| Generate[Generate New Response]
    
    Generate --> Store[Store in Cache]
    Store --> ReturnNew[Return New Response]
    
    subgraph "Cache Key Components"
        Method[HTTP Method]
        Path[Request Path]
        Scenario[Scenario Header]
        Seed[Seed Header]
        Body[Request Body Hash]
    end
    
    Method --> CacheKey
    Path --> CacheKey
    Scenario --> CacheKey
    Seed --> CacheKey
    Body --> CacheKey
    
    CacheKey --> KeyGen

    style Request fill:#fff3e0
    style ReturnCached fill:#e8f5e9
    style ReturnNew fill:#e8f5e9
    style Generate fill:#ffebee
```

## OpenAPI Spec Processing

```mermaid
graph TD
    Upload[Upload OpenAPI Spec]
    
    Upload --> Parse{Parse YAML/JSON}
    Parse -->|Success| Index[Index Endpoints]
    Parse -->|Failed| Error[Return Error]
    
    Index --> ExtractPaths[Extract Paths]
    ExtractPaths --> ExtractOps[Extract Operations]
    ExtractOps --> ExtractSchemas[Extract Schemas]
    ExtractSchemas --> ResolveRefs["Resolve refs"]
    ResolveRefs --> BuildIndex[Build Endpoint Index]
    
    BuildIndex --> Ready[Ready to Serve]
    
    subgraph IndexedData["Indexed Data Structure"]
        PathPattern["Path Pattern<br/>/api/pets/{id}"]
        HTTPMethod["HTTP Methods<br/>GET, POST, PUT, DELETE"]
        ResponseSchemas["Response Schemas<br/>200, 400, 500"]
        Parameters["Parameters<br/>Path, Query, Body"]
    end
    
    BuildIndex --> PathPattern
    BuildIndex --> HTTPMethod
    BuildIndex --> ResponseSchemas
    BuildIndex --> Parameters

    style Upload fill:#e1f5fe
    style Ready fill:#e8f5e9
    style Error fill:#ffebee
```

## Deployment Architecture

```mermaid
graph TB
    subgraph "Local Development"
        DevMachine[Developer Machine]
        LocalOllama[Ollama<br/>localhost:11434]
        LocalApp[Smart Mock<br/>localhost:8080]
        
        DevMachine --> LocalApp
        LocalApp --> LocalOllama
    end
    
    subgraph "Team Deployment"
        TeamServer[Shared Server]
        SharedOllama[Ollama Service<br/>GPU Accelerated]
        SharedApp[Smart Mock<br/>team.example.com]
        
        Developer1[Developer 1]
        Developer2[Developer 2]
        Developer3[Developer 3]
        
        Developer1 --> SharedApp
        Developer2 --> SharedApp
        Developer3 --> SharedApp
        SharedApp --> SharedOllama
    end
    
    subgraph "Container Deployment"
        DockerCompose[Docker Compose]
        
        AppContainer[Smart Mock Container<br/>Spring Boot]
        OllamaContainer[Ollama Container<br/>LLM Server]
        
        DockerCompose --> AppContainer
        DockerCompose --> OllamaContainer
        AppContainer --> OllamaContainer
    end

    style DevMachine fill:#e1f5fe
    style TeamServer fill:#f3e5f5
    style DockerCompose fill:#e8f5e9
```

## Technology Stack

```mermaid
mindmap
  root((Smart Mock))
    Backend
      Spring Boot 3.3.2
        Spring Web
        Spring Validation
        Configuration Properties
      LangChain4j 1.3.0
        Ollama Integration
        Language Model API
        Prompt Templates
    
    AI/ML
      Ollama
        CodeLlama 7B
        Mistral 7B
        Local Inference
      Response Generation
        Prompt Engineering
        JSON Generation
        Schema Validation
    
    API Specs
      OpenAPI 3.0
        Swagger Parser 2.1
        Schema Resolution
        Path Matching
      Swagger UI
        Interactive Explorer
        Try It Out
        Documentation
    
    Caching
      Caffeine 3.1.8
        In-Memory Cache
        TTL Management
        Size Limits
    
    Frontend
      Thymeleaf
        Server-Side Rendering
        Dynamic Content
      Bootstrap 5
        Responsive Design
        Components
      JavaScript
        File Upload
        API Testing
```

## Security Considerations

```mermaid
graph TD
    subgraph "Security Layers"
        Input[Input Validation]
        Sanitize[Response Sanitization]
        LocalLLM[Local LLM<br/>No Data Leakage]
        CORS[CORS Configuration]
        RateLimit[Rate Limiting<br/>via Headers]
    end
    
    subgraph "Privacy Benefits"
        NoAPI[No External APIs]
        NoKeys[No API Keys]
        LocalData[Data Stays Local]
        Control[Full Control]
    end
    
    Input --> Sanitize
    Sanitize --> LocalLLM
    LocalLLM --> LocalData
    
    NoAPI --> Control
    NoKeys --> Control
    
    style LocalLLM fill:#e8f5e9
    style LocalData fill:#e8f5e9
    style Control fill:#e8f5e9
```

These diagrams provide a comprehensive view of the Smart Mock Server architecture, showing:

1. **High-Level Architecture**: Overall system components and their relationships
2. **Request Flow**: Detailed sequence of how requests are processed
3. **Component Pipeline**: The response generation pipeline
4. **State Management**: How scenarios affect response generation
5. **Caching Strategy**: How responses are cached and retrieved
6. **OpenAPI Processing**: How specifications are parsed and indexed
7. **Deployment Options**: Various deployment configurations
8. **Technology Stack**: Mind map of all technologies used
9. **Security Considerations**: Privacy and security benefits

You can include these diagrams in your article or documentation. They render beautifully in any Markdown viewer that supports Mermaid (GitHub, GitLab, many documentation tools, etc.).