# AI Agent Platform: Agent Catalog

This document provides a comprehensive description of all autonomous AI agents and pipelines implemented within the
platform. The agents are organized into a hierarchy (from L5 to L2) based on their level of autonomy, complexity, and
scope of responsibility.

## Agent Hierarchy

- **L5 (Executive Governors):** Strategic meta-agents operating at the level of the entire project portfolio. They
  accept business goals, analyze aggregated data, and formulate high-level roadmaps.
- **L4 (Orchestrators & Commanders):** Meta-agents responsible for solving a single, complex, multi-step task. They
  decompose a goal, build a dynamic execution plan (DAG) from L2/L3 agents, and oversee its execution.
- **L3 (Specialists):** Agents with deep expertise in a narrow domain (e.g., security analysis, performance evaluation).
  They perform complex analysis and generate reports or artifacts.
- **L2 (Tools):** Atomic, deterministic, or AI-driven "tools" that perform one simple, well-defined function (e.g., get
  a diff from Git, create a Jira ticket, parse code).

## L5: Executive Governors

These agents act as "virtual C-level executives," providing strategic insights across the entire engineering department.

| Agent / Pipeline                                                          | Purpose                                                                                                                                                                                                       |
|:--------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **AI CTO / Strategic Initiative Planner** (`executive-governor-pipeline`) | Analyzes the technical health and risks across **all projects** in the portfolio. Formulates a strategic technical roadmap for the next quarter to address systemic issues.                                   |
| **AI VP of Engineering** (`engineering-velocity-pipeline`)                | Analyzes SDLC metrics (DORA, Git, Jira) to assess the efficiency of engineering processes. Identifies bottlenecks and proposes strategic improvements.                                                        |
| **AI CFO / ROI Analyst** (`financial-roi-analysis-pipeline`)              | Analyzes all types of costs (infrastructure, LLM, development) and correlates them with the value of implemented features. Calculates ROI for engineering initiatives and provides financial recommendations. |
| **AI CPO / Product Portfolio Strategist** (`product-strategy-pipeline`)   | Synthesizes user feedback and competitor analysis data to formulate a high-level product strategy and define key initiatives.                                                                                 |

## L4: Orchestrators & Commanders

These agents are the "brains" of the system, capable of solving complex tasks that require the coordination of multiple
tools.

| Agent / Pipeline                                                     | Purpose                                                                                                                                                                                                      |
|:---------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **QA Copilot** (`QaCopilotService`)                                  | A stateful assistant that manages an interactive dialogue with the user. It maintains context between messages, invokes the `WorkflowPlannerAgent` to execute tasks, and summarizes the results.             |
| **Workflow Planner & Executor**                                      | The core of dynamic orchestration. It takes a high-level goal in natural language, builds a directed acyclic graph (DAG) of L2/L3 agents, and executes it, respecting dependencies and enabling parallelism. |
| **Root Cause Analyzer** (`root-cause-analysis-pipeline`)             | Performs a comprehensive root cause analysis of test failures by synthesizing data from logs, code diffs, and flaky test reports to deliver a verdict.                                                       |
| **Incident Commander** (`incident-response-pipeline`)                | Reacts to alerts from monitoring systems. It triages the incident by analyzing recent changes and logs, then launches a containment plan (e.g., rolling back a release via the `CI Trigger`).                |
| **Policy & Safety Governor** (`policy-and-safety-governor-pipeline`) | Acts as an automated Quality Gate. It orchestrates all policy compliance checks (SAST, architecture, privacy, licensing) and aggregates them into a single verdict.                                          |
| **Knowledge Consistency Guardian** (`knowledge-guardian-pipeline`)   | Proactively (on a schedule) scans the knowledge base, comparing semantically similar documents to detect internal contradictions, and automatically creates tickets to resolve them.                         |
| **Test DataOps Orchestrator** (`test-data-ops-pipeline`)             | Analyzes a natural language request for test data, selects the appropriate L2/L3 generator agent, and executes it.                                                                                           |
| **Prioritization Agent** (`prioritization-pipeline`)                 | An "AI Tech Lead" that gathers all project health reports, analyzes them in the context of a business goal, and generates a prioritized backlog.                                                             |

## L3: Specialists

These agents perform in-depth analysis within their specific domain and generate structured reports.

| Category                 | Agent                             | Purpose                                                                                                                                       |
|:-------------------------|:----------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------|
| **Security**             | `SastAgent`                       | Performs Static Application Security Testing (SAST) on Java code to find vulnerabilities from the OWASP Top 10 list.                          |
|                          | `AuthRiskDetectorAgent`           | Analyzes extracted RBAC rules for potential risks, such as violations of the principle of least privilege.                                    |
|                          | `SecurityReportAggregatorAgent`   | An "AI CISO" that aggregates reports from all security scanners into a single, prioritized verdict.                                           |
| **Analytics**            | `TestDebtAnalyzerAgent`           | Analyzes test run history to identify flaky and slow tests, creating a comprehensive report on test-related technical debt.                   |
|                          | `KnowledgeGapClustererAgent`      | Analyzes unanswered user queries, clusters them by topic, and suggests titles for new knowledge base articles.                                |
|                          | `PerformancePredictorAgent`       | Analyzes code changes (complexity, defect history, patterns) to predict their potential impact on performance (latency, CPU, memory).         |
|                          | `CustomerImpactAnalyzerAgent`     | Analyzes code diffs and commit history to assess the impact on end-users (UI, API, business logic).                                           |
|                          | `CodeQualityImpactEstimatorAgent` | Assesses regression risk by analyzing static code metrics and historical defect data.                                                         |
|                          | `DefectEconomicsModelerAgent`     | Estimates the cost of remediation and the cost of inaction for a defect or risky piece of code.                                               |
| **UI/UX**                | `AccessibilityAuditorAgent`       | Orchestrates an audit of HTML for compliance with accessibility (a11y) standards.                                                             |
|                          | `UxHeuristicsEvaluatorAgent`      | Evaluates HTML code against Jakob Nielsen's 10 usability heuristics and generates a report with recommendations.                              |
|                          | `UserBehaviorSimulatorAgent`      | Autonomously simulates an E2E user scenario by controlling a web browser via Playwright.                                                      |
| **Contracts**            | `DataContractEnforcerAgent`       | Compares two versions of a Java DTO to identify breaking changes in the data contract.                                                        |
| **Generation**           | `TestDesignerAgent`               | The "Builder Agent" that generates a positive "happy path" test case from requirements.                                                       |
|                          | `AdversarialTesterAgent`          | The "Breaker Agent" that analyzes the existing test and requirements to find missed scenarios and generate negative/edge case tests for them. |
|                          | `AuthTestBuilderAgent`            | Generates API test code to verify RBAC rules.                                                                                                 |
|                          | `FuzzingTestGeneratorAgent`       | Generates fuzzing test code to simulate an IDOR attack based on personas and RBAC rules.                                                      |
|                          | `SelfImprovingTestDesignerAgent`  | Generates unit tests by learning from existing examples within the project's codebase.                                                        |
| **Explainability (XAI)** | `ExplainerAgent`                  | The "Explainer Agent" that translates a technical report from another agent into natural language in response to a user's question.           |
|                          | `TestMentorAgent`                 | Acts as an AI mentor, providing a detailed code review of an automated test against requirements and best practices.                          |
|                          | `XaiTestOracleAgent`              | The "Test Oracle" that builds a traceability matrix between tests and requirements, identifying coverage gaps.                                |
| **Optimization**         | `RagOptimizerAgent`               | An "AI MLOps Engineer" that analyzes RAG system performance and suggests configuration improvements.                                          |
|                          | `ErrorHandlerAgent`               | An "AI SRE" that diagnoses failures in other agents and attempts to automatically remediate them.                                             |

## L2: Tools & Fetchers

These are atomic agents that perform one specific, well-defined task. They serve as the building blocks for higher-level
agents.

| Category               | Agent                       | Purpose                                                                                                    | Approval Required? |
|:-----------------------|:----------------------------|:-----------------------------------------------------------------------------------------------------------|:-------------------|
| **System Interaction** | `GitInspectorAgent`         | Extracts the list of changed files, diffs, or commit history from a Git repository between two references. | No                 |
|                        | `WebCrawlerAgent`           | Extracts and cleans the text content from a web page given its URL.                                        | No                 |
|                        | `JiraTicketCreatorAgent`    | Creates a ticket in Jira with specified parameters.                                                        | **Yes**            |
|                        | `JiraFetcherAgent`          | Fetches the details of a Jira ticket by its ID.                                                            | No                 |
|                        | `CiTriggerAgent`            | Triggers a job in a CI/CD system (e.g., Jenkins).                                                          | **Yes**            |
|                        | `DeploymentRollbackAgent`   | Triggers a specialized CI/CD job to roll back a release.                                                   | **Yes**            |
| **Code Analysis**      | `CodeParserAgent`           | Performs Abstract Syntax Tree (AST) analysis of a Java file to extract its structure (methods, classes).   | No                 |
|                        | `RbacExtractorAgent`        | Extracts Role-Based Access Control (RBAC) rules from Spring Security annotations in Java code.             | No                 |
|                        | `StaticCodeAnalyzerService` | A deterministic analyzer that calculates code metrics (e.g., cyclomatic complexity).                       | No                 |
| **Data Analysis**      | `TestMetricsCollectorAgent` | Parses JUnit XML reports and saves the aggregated metrics to the database.                                 | No                 |
|                        | `FlakinessTrackerAgent`     | Analyzes test run history to identify flaky tests.                                                         | No                 |
|                        | `CiCdMetricsFetcherAgent`   | *[Mock]* Collects DORA metrics from a CI/CD system.                                                        | No                 |
|                        | `GitMetricsFetcherAgent`    | *[Mock]* Collects metrics about Pull Requests from Git.                                                    | No                 |
|                        | `JiraMetricsFetcherAgent`   | *[Mock]* Collects issue lifecycle metrics from Jira.                                                       | No                 |
|                        | `CloudCostFetcherAgent`     | *[Mock]* Collects data on cloud infrastructure costs.                                                      | No                 |
| **Generation**         | `PersonaGeneratorAgent`     | Generates "attacker personas" based on RBAC rules for use in fuzzing tests.                                | No                 |
|                        | `SyntheticDataBuilderAgent` | Generates mock JSON objects based on a Java class definition.                                              | No                 |
|                        | `DataSubsetMaskerAgent`     | Generates SQL to create a subset of data from a database and masks PII.                                    | **Yes**            |
| **RAG Optimization**   | `QueryProfilerAgent`        | Determines the semantic profile of a query to select the optimal RAG strategy.                             | No                 |
|                        | `KnowledgeRouterAgent`      | Selects the most relevant "knowledge domain" to search for an answer.                                      | No                 |
|                        | `ReflectiveRetrieverAgent`  | Implements a self-correcting retrieval loop by rephrasing the query if necessary.                          | No                 |
|                        | `DocumentEnhancerAgent`     | Enriches documents in the knowledge base with metadata (summary, keywords).                                | No                 |
| **Infrastructure**     | `HumanInTheLoopGateAgent`   | Pauses a pipeline to await human approval for the next step.                                               | **Yes**            |
