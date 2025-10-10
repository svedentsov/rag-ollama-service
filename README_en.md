# Autonomous AI Agent Platform for SDLC & QA

This project is a **production-grade platform** for integrating, orchestrating, and operating autonomous AI agents within the Software Development Life Cycle (SDLC) and Quality Assurance (QA) processes. The system is built on Spring Boot 3.3+ (WebFlux) and Java 21.

This is not just a RAG prototype, but a comprehensive solution built on state-of-the-art architectural patterns, including:
*   **A hierarchy of autonomous AI agents (L2-L5)** capable of solving tasks from code analysis to strategic planning.
*   **An advanced, multi-stage RAG pipeline** that implements cutting-edge techniques for maximum accuracy and hallucination mitigation.
*   **A full MLOps cycle for the knowledge base**, including CI/CD, drift monitoring, and automatic regression test generation from user feedback.
*   **Dynamic orchestration** that allows building execution plans (DAGs) on-the-fly from available tools to achieve high-level goals.

The system utilizes locally deployed LLMs via Ollama, a `pgvector` vector database, and a `Neo4j` graph database for constructing knowledge graphs.

## Core Architectural Principles

1.  **Agent-Oriented Architecture:** A hierarchy of AI agents at different levels (from L2 "tools" to L5 "AI governors") allows for the decomposition of complex tasks and the creation of flexible, reusable components.
2.  **Dynamic Orchestration (DAG Execution):** Instead of static pipelines, the system includes an AI planner (`WorkflowPlannerAgent`) that constructs a Directed Acyclic Graph (DAG) of available agents to achieve a goal specified in natural language. This enables parallel execution of independent tasks.
3.  **Explainable AI (XAI) & Human-in-the-Loop:** Key components can explain their actions (`ExplainerAgent`), and critical operations (creating tickets, triggering CI, rolling back releases) require explicit human approval via a dedicated API.
4.  **Full MLOps Cycle for the Knowledge Base:** The knowledge base is treated as a critical artifact, managed with the same principles as code:
    *   **CI/CD for Knowledge:** Automatic execution of tests against a "golden dataset" after each indexing to prevent regressions (`knowledge-ci-cd-pipeline`).
    *   **Drift Monitoring:** Automatic detection of changes in the embedding model to trigger timely re-indexing.
    *   **Proactive Curation:** Background AI agents (`KnowledgeGuardianAgent`) constantly check the knowledge base for internal contradictions and suggest improvements.

## Architectural Diagram

```mermaid
graph TD
    subgraph "User & External Systems"
        A[User / API Client]
        B[CI/CD, Jira, GitHub Webhooks]
    end

    subgraph "rag-app (Spring Boot WebFlux)"
        C[API Gateway / Controllers]
        D[Universal Orchestrator]
        E[Dynamic Planner (L4)]
        F[DAG Executor]
        G[AI Agent Registry (L2-L3)]
        H[Advanced RAG Pipeline]
        I[External System Clients]
    end

    subgraph "Backend Services"
        J[Ollama LLM]
        K[PostgreSQL / pgvector]
        M[Neo4j Knowledge Graph]
    end

    A --> C
    B --> C
    C --> D
    D -- "Goal in natural language" --> E
    D -- "Simple RAG query" --> H
    E -- "Plan (DAG)" --> F
    F --> G
    G --> I
    H --> I
    I --> J
    I --> K
    I --> M
```

## The RAG Pipeline: From Query to Verified Answer

The system implements a multi-stage RAG pipeline that significantly surpasses standard implementations:

1.  **Input Guardrails:** `PromptGuardStep` analyzes each query for **Prompt Injection** and blocks potential attacks.
2.  **Query Processing:** `QueryProcessingPipeline` applies a cascade of techniques:
    *   **Hypothetical Document Embeddings (HyDE):** Generates a "perfect" answer to the query for more accurate semantic search.
    *   **Multi-Query & Step-Back:** Creates multiple query variations to improve retrieval recall.
3.  **Self-Correcting Retrieval:** `ReflectiveRetrieverAgent` performs a hybrid search (vector + FTS), and then an **AI Critic** evaluates the retrieved documents. If they are irrelevant, the agent reformulates the query and searches again.
4.  **Context Expansion:**
    *   **Graph RAG:** `GraphExpansionStep` queries Neo4j to find related entities (tests, requirements) and enriches the context.
    *   **Parent Document Retriever:** `ContextExpansionStep` replaces retrieved "child" chunks with their full "parent" documents, solving the "lost in the middle" problem.
5.  **Advanced Reranking:**
    *   **MMR (Maximal Marginal Relevance):** `DiversityRerankingStrategy` increases document diversity in the context to combat redundancy.
    *   **Cross-Encoder Reranker:** Final sorting by relevance using a powerful local model (DJL/PyTorch).
6.  **Chain-of-Thought Generation:**
    *   The final prompt forces the LLM to first extract all relevant facts with source citations, and only then synthesize an answer based on them.
7.  **Output Guardrails:**
    *   **AI Critic (`ResponseValidationStep`):** An "independent" AI agent checks the generated answer for hallucinations, completeness, and correct citation.
    *   **Trust Score:** A "Trust Score" is calculated for each answer and returned to the client via the API.

## Agent Catalog

| Level | Agent / Pipeline | Purpose |
|:--- |:--- |:--- |
| **L5 (Governors)** | **AI CTO / Executive Governor** | Analyzes the health of all projects in the portfolio and forms a strategic technical roadmap. |
| | **AI VP of Engineering** | Analyzes SDLC metrics (DORA, Git) and suggests improvements for engineering processes. |
| | **AI CFO / ROI Analyst** | Analyzes costs (infrastructure, LLM, development) and calculates ROI for engineering initiatives. |
| **L4 (Orchestrators)** | **QA Copilot** | A stateful assistant managing dialogue and dynamic task execution. |
| | **Root Cause Analyzer** | Conducts a comprehensive root cause analysis of test failures, synthesizing data from logs, diffs, and reports. |
| | **Incident Commander** | Responds to monitoring alerts, performs triage, and executes an incident containment plan (e.g., release rollback). |
| | **Knowledge Consistency Guardian** | Proactively checks the knowledge base for internal contradictions between different sources. |
| **L3 (Specialists)**| **SAST & RBAC Security Agents** | Perform static code analysis for vulnerabilities (OWASP) and access control misconfigurations. |
| | **Test Debt Analyzer** | Analyzes test run history to identify flaky and slow tests, generating a tech debt report. |
| | **Knowledge Gap Advisor** | Analyzes unanswered queries and suggests topics for new documentation. |
| | **Performance Predictor** | Predicts the impact of code changes on performance (latency, CPU, memory). |
| | **UX Heuristics Evaluator**| Evaluates HTML code against Jakob Nielsen's 10 usability heuristics. |
| **L2 (Tools)**| **Git Inspector** | Extracts changed files, diffs, and commit history from Git. |
| | **Code Parser** | Performs AST analysis of Java code to extract its structure. |
| | **Web Crawler** | Extracts text content from external web pages for analysis. |
| | **Jira Ticket Creator**| Creates a ticket in Jira (requires human approval). |
| | **CI Trigger**| Triggers a job in a CI/CD system (requires human approval). |

## Technology Stack

| Category | Technology | Purpose |
|:--- |:--- |:--- |
| **Core** | Java 21 & Spring Boot 3.3+ (WebFlux) | Main framework and language. |
| **AI & Agents** | Spring AI, Ollama, DJL/PyTorch | Integration with LLMs, vector stores, and local ML models. |
| **Databases** | PostgreSQL + `pgvector`, Neo4j | Relational, vector, and graph storage. |
| **Resilience** | Resilience4j | Implementation of Circuit Breaker, Retry, TimeLimiter patterns. |
| **Observability**| Actuator, Micrometer, Prometheus | Health monitoring and metrics collection. |
| **Testing** | JUnit 5, Mockito, Testcontainers, Awaitility | Unit and integration testing. |
| **Tooling** | JGit, PMD, JavaParser, Playwright | Code analysis and UI automation. |

## Quick Start

### Prerequisites

1.  **Docker and Docker Compose**
2.  **Java 21+ SDK**
3.  **Git**

### Launch and Setup

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/svedentsov/rag-ollama-service.git
    cd rag-ollama-service
    ```

2.  **Start the entire infrastructure:**
    This command will build the application image and start all containers (`rag-app`, `postgres`, `ollama`, `neo4j`).
    ```bash
    docker-compose up --build -d
    ```

3.  **Pull LLM models into Ollama:**
    ```bash
    # Model for balanced tasks (RAG, chat, planning)
    docker exec -it rag-ollama ollama pull llama3

    # Fast model for classification and routing
    docker exec -it rag-ollama ollama pull phi3

    # Model for embeddings
    docker exec -it rag-ollama ollama pull mxbai-embed-large
    ```
    *Model names can be changed in `application.yml` under `app.llm.models`.*

4.  **Verify that the services are running:**
    ```bash
    docker ps
    curl http://localhost:8080/actuator/health
    ```
    Expected response: `{"status":"UP"}`.

## API Usage

The main entry point is the **Universal Orchestrator**, which automatically determines the user's intent and launches the appropriate pipeline.

1.  **Open Swagger UI in your browser:**
    [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

2.  **Use the `POST /api/v1/workflows/execute` endpoint for complex tasks:**
    This endpoint allows you to specify a goal in natural language. The AI planner will build and execute a plan to achieve it.

    *   **Example: Full security audit for changes**
      ```json
      {
        "goal": "Perform a full security audit for changes between main and feature/new-security-logic",
        "initialContext": {
          "oldRef": "main",
          "newRef": "feature/new-security-logic",
          "privacyPolicy": "Logging PII (email, name, phone) is prohibited."
        }
      }
      ```
    The response will contain an aggregated report from the `SecurityReportAggregatorAgent`.

3.  **Use the `POST /api/v1/orchestrator/ask-stream` endpoint for RAG queries:**
    This endpoint is optimal for chat interfaces and RAG queries as it returns the response in a streaming fashion.
    *   **Example: Simple RAG query**
      ```json
      {
        "query": "How many vacation days are employees entitled to per year?"
      }
      ```
    The response will be delivered in parts (SSE), including the generated text and the list of sources.