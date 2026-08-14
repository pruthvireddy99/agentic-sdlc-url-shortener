# Governed Agentic URL Shortener

A production-minded Java 21 / Spring Boot prototype for an SDLC automation assignment. The system combines a working URL shortener with a stateful agentic orchestration control plane that demonstrates:

- requirement normalization and ambiguity handling
- dependency-graph based task decomposition
- parallel execution with synchronization
- cross-stage context and decision lineage
- human approval for high-impact release actions
- bounded retries and policy-bounded fallback
- dynamic replanning when upstream assumptions change
- rollback and safe-stop controls
- audit-grade workflow observability
- reliability metrics through Micrometer/Actuator
- runnable APIs, persistence, migrations, tests, Docker, and documentation

## Why the prototype is deliberately deterministic

The assignment evaluates engineering judgment and controlled agentic execution, not whether the evaluator happens to have an LLM API key. The agent layer is implemented behind a clean execution seam and uses deterministic local agent handlers by default.

This makes runs reproducible and testable. A model-backed adapter can replace the deterministic handlers without changing the orchestration state machine.

The orchestration layer is the differentiator: it is a stateful workflow engine, not a linear list of prompts.

## Technology

- Java 21
- Spring Boot 3.5.16
- Spring Web / Validation / Data JPA / Actuator
- PostgreSQL 16
- H2 for the zero-infrastructure local profile and tests
- Flyway
- Micrometer / Prometheus
- Maven
- JUnit 5 / Mockito
- Optional PostgreSQL Testcontainers support
- Docker / Docker Compose

## Architecture

```text
                         +---------------------------+
                         |       REST Clients        |
                         +-------------+-------------+
                                       |
                    +------------------+------------------+
                    |                                     |
                    v                                     v
          +-------------------+                +----------------------+
          |   URL Shortener   |                | Agent Control Plane  |
          | create / redirect |                | run / approve /      |
          | analytics         |                | clarify / replan /   |
          +---------+---------+                | stop / rollback      |
                    |                          +----------+-----------+
                    v                                     |
          +-------------------+                            v
          | Domain Services   |                +----------------------+
          +----+---------+----+                | Stateful Graph       |
               |         |                     | Orchestrator         |
               v         v                     +----------+-----------+
        +---------+ +---------+                           |
        | Short   | | Click   |                           v
        | URLs    | | Events  |                +----------------------+
        +----+----+ +----+----+                | Agent Executor Layer |
             |           |                     | deterministic by     |
             +-----+-----+                     | default; LLM seam    |
                   |                           +----------+-----------+
                   v                                      |
          +-------------------------+                     |
          | PostgreSQL + Flyway      |<--------------------+
          | + persisted audit log    |
          +-------------------------+

Observability: Actuator + Micrometer metrics + workflow audit events
Governance: control token + approval token + node policy + bounded retries
```

## Orchestration model

The orchestration state is persisted. Node status, attempts, outputs, timestamps, dependency edges, shared context, and decision lineage are durable, making workflow execution reviewable and resumable within the prototype's supported state model.

### Greenfield

```text
Requirements
     |
     v
Architecture
   /      \
  v        v
Implementation   Documentation
      \           /
       v         v
          Testing
             |
             v
      Release Readiness
             |
             v
       HUMAN APPROVAL  <-- hard gate
             |
             v
           Release
```

### Brownfield

```text
Requirements
      |
      v
Codebase Reasoning
      |
      v
Impact Analysis
      |
      v
Architecture
    /       \
   v         v
Implementation   Documentation
    \             /
     v           v
         Testing
            |
            v
     Release Readiness
            |
            v
      HUMAN APPROVAL
            |
            v
          Release
```

### Ambiguous

```text
Requirements
      |
      v
Ambiguity Review
      |
      v
HUMAN CLARIFICATION
      |
      v
Architecture
    /       \
   v         v
Implementation   Documentation
    \             /
     v           v
         Testing
            |
            v
     Release Readiness
            |
            v
      HUMAN APPROVAL
            |
            v
          Release
```

When clarification changes an upstream assumption, the orchestrator records the clarification in shared context and decision lineage, increments the plan version, and re-executes the affected downstream path.

## SDLC governance model

### Autonomous

Low-risk analysis, architecture, implementation planning, documentation generation, and validation are eligible for autonomous execution.

### Human controlled

Release approval is always a human checkpoint. Ambiguous requirements stop for clarification. Rollback and safe-stop are explicit human-controlled actions.

The prototype never autonomously mutates cloud infrastructure or deployment credentials.

### Bounded failure handling

- each executable node has a retry budget
- retries are persisted as attempts
- documentation is the policy-bounded fallback path in this prototype
- safe-stop blocks outstanding work
- rollback transitions the release outcome to a terminal rolled-back state
- maximum run duration prevents runaway execution

## Security and controlled autonomy

The agent control plane is intentionally separated from the public URL APIs.

All `/api/v1/agent-runs/**` endpoints require:

```text
X-Agent-Control-Token: local-demo-control-token
```

High-impact release approval additionally requires:

```text
approvalToken: local-demo-approval-token
```

These values are local/demo credentials only. They are not intended for production deployment.

For enterprise deployment, replace static demo tokens with OIDC/JWT-based authorization and policy enforcement.

## API surface

### URL shortener

Create:

```text
POST /api/v1/urls
```

Example request:

```json
{
  "originalUrl": "https://example.com/some/long/path",
  "ttlHours": 24
}
```

Optional header:

```text
Idempotency-Key: customer-request-123
```

Redirect:

```text
GET /{code}
```

The redirect records a click event.

Analytics:

```text
GET /api/v1/urls/{code}/analytics
```

Returns total clicks plus last-24-hour and last-7-day aggregates.

### Agent control plane

Create a run:

```text
POST /api/v1/agent-runs
```

Read run state:

```text
GET /api/v1/agent-runs/{id}
```

Read audit history:

```text
GET /api/v1/agent-runs/{id}/audit
```

Approve:

```text
POST /api/v1/agent-runs/{id}/approve
```

Clarify:

```text
POST /api/v1/agent-runs/{id}/clarify
```

Replan:

```text
POST /api/v1/agent-runs/{id}/replan
```

Fallback:

```text
POST /api/v1/agent-runs/{id}/fallback
```

Stop:

```text
POST /api/v1/agent-runs/{id}/stop
```

Rollback:

```text
POST /api/v1/agent-runs/{id}/rollback
```

A complete API definition is available in:

```text
docs/openapi.yaml
```

## Local setup

The repository provides two supported runtime paths.

### Option A: Zero-infrastructure local run (recommended for evaluation)

This path uses the `local` Spring profile with a file-backed H2 database. Docker and PostgreSQL are not required.

#### Windows

Run tests:

```cmd
mvnw.cmd clean test
```

Start the application:

```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Health check:

```cmd
curl http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP",
  "groups": [
    "liveness",
    "readiness"
  ]
}
```

#### macOS / Linux

Run tests:

```bash
./mvnw clean test
```

Start the application:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

The local profile does not require PostgreSQL, Flyway, or Docker.

### Option B: Docker Compose / PostgreSQL

For the production-style containerized path:

```bash
docker compose up --build
```

The application becomes available at:

```text
http://localhost:8080
```

Health:

```bash
curl http://localhost:8080/actuator/health
```

The PostgreSQL deployment uses environment-driven configuration. Set `POSTGRES_PASSWORD` before starting the production-style Docker Compose stack.

Example on macOS / Linux:

```bash
export POSTGRES_PASSWORD=<your-password>
docker compose up --build
```

Example on Windows PowerShell:

```powershell
$env:POSTGRES_PASSWORD="<your-password>"
docker compose up --build
```

## Observability

Health/readiness:

```text
GET /actuator/health
```

Prometheus metrics:

```text
GET /actuator/prometheus
```

The application exposes standard Spring/JVM/database metrics plus orchestration-specific Micrometer metrics for workflow activity, node success/latency, approvals, completion, replanning, safe-stop, rollback, fallback, retry, and recovery behavior as those lifecycle events occur.

Example Windows command:

```cmd
curl -s http://localhost:8080/actuator/prometheus | findstr /b "agentic_"
```

The persisted audit endpoint provides workflow-level traceability:

```text
GET /api/v1/agent-runs/{id}/audit
```

## End-to-end demo

The included `scripts/demo.sh` is a walkthrough for the assignment scenarios and API flow.

macOS / Linux:

```bash
chmod +x scripts/demo.sh
./scripts/demo.sh
```

On Windows, the same API calls can be executed with the documented `curl` commands in:

```text
docs/api-examples.md
docs/runbook.md
```

Recommended demonstration order:

1. Create and redirect a short URL.
2. Query analytics.
3. Demonstrate idempotent URL creation.
4. Run the Greenfield scenario and stop at the human approval gate.
5. Approve and verify completion.
6. Run the Ambiguous scenario and verify the clarification gate.
7. Supply clarification and verify `planVersion` increments and downstream work resumes.
8. Run the Brownfield scenario and inspect codebase reasoning and impact analysis.
9. Demonstrate safe-stop.
10. Demonstrate rollback on a rollback-eligible state.
11. Inspect audit history.
12. Inspect Prometheus orchestration metrics.

## Testing

The default test profile uses H2 for fast, deterministic unit/context testing.

### Windows

```cmd
mvnw.cmd clean test
```

### macOS / Linux

```bash
./mvnw clean test
```

The dependency set also includes PostgreSQL Testcontainers support so integration coverage can be added without changing the application data-access design.

The test suite covers core URL behavior and orchestration behavior including dependency gating, ambiguity/clarification handling, approval gating, safe-stop, replanning, and observability configuration.

## Production evolution path

For an enterprise deployment, the next increments would be:

1. OIDC/JWT authorization for the agent control plane instead of static demo tokens.
2. Kafka-backed domain event publication for click events and orchestration events.
3. Redis cache for hot short-code resolution with cache invalidation on expiry.
4. External durable workflow orchestration if runs need cross-region execution.
5. Real LLM adapters for Claude/OpenAI/Azure OpenAI behind the agent executor interface.
6. Policy-as-code evaluation for security, compliance, and change-management rules.
7. OpenTelemetry traces correlated with persisted workflow/audit identifiers.
8. Blue/green or canary deployment integration behind an additional human approval gate.

## Assignment coverage

| Assignment expectation | Implementation |
|---|---|
| Requirement understanding | Requirements node + normalized problem + acceptance criteria + ambiguity list |
| Task decomposition | Explicit persisted dependency graph |
| Brownfield reasoning | Codebase reasoning + impact analysis nodes |
| Non-linear orchestration | Parallel implementation/documentation branches + testing synchronization |
| Stateful execution | Persistent run/node/context tables |
| Cross-stage context | JSON context + decision lineage |
| Human oversight | Clarification gate + release approval gate |
| Retries | Per-node bounded retry budget |
| Fallback | Policy-bounded documentation fallback |
| Rollback | Explicit rollback control and terminal state |
| Safe stop | Explicit stop control blocking outstanding nodes |
| Dynamic replanning | Plan version + downstream invalidation + re-execution |
| Guardrails | Control token, approval token, bounded duration, node policy |
| Auditability | Persisted append-only-style audit records by application design |
| Reliability metrics | Micrometer counters/timers + Actuator |
| Production code quality | Transactions, validation, persistence, migrations, tests, Docker |
| Final summary | `docs/final-engineering-summary.md` |

## Important design limitations

The prototype intentionally does not claim that an LLM is safe merely because it generated code.

Model output is treated as untrusted work product. The orchestrator controls:

- what can run
- when it can run
- what must stop
- what must be validated
- what requires human approval

The deterministic local agents are used for reproducibility. A model-backed implementation can be introduced through the agent execution abstraction without changing the core orchestration state machine.

The reliability metrics in the prototype measure lifecycle events observed by the application process. A production implementation should additionally persist incident and recovery timestamps so MTTR-style calculations remain accurate across service restarts and deployments.
