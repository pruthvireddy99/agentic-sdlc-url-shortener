# Governed Agentic URL Shortener

A production-minded Java 21 / Spring Boot prototype for the SDLC automation assignment. The system combines a working URL shortener with a stateful agentic orchestration control plane that demonstrates:

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

The assignment evaluates engineering judgment and controlled agentic execution, not whether the evaluator happens to have an LLM API key. The agent layer is therefore implemented behind a clean execution seam and uses deterministic local agent handlers by default. This makes runs reproducible and testable. A model-backed adapter can replace the deterministic handlers without changing the orchestration state machine.

The orchestration layer is the differentiator: it is a stateful workflow engine, not a linear list of prompts.

## Technology

- Java 21
- Spring Boot 3.5.16
- Spring Web / Validation / Data JPA / Actuator
- PostgreSQL 16
- Flyway
- Micrometer
- Maven
- JUnit 5 / Mockito / optional Testcontainers

Spring Boot 3.5.16 was selected because it is a current 3.5 service release and is the final OSS release in that generation; the design intentionally stays on the mature 3.x programming model for a small, reviewable assignment prototype.

## Architecture

```text
                         +---------------------------+
                         |       REST Clients        |
                         +-------------+-------------+
                                       |
                +----------------------+----------------------+
                |                                             |
                v                                             v
      +-------------------+                         +----------------------+
      |  URL Shortener    |                         | Agent Control Plane  |
      | create / redirect |                         | run / approve /      |
      | analytics         |                         | clarify / replan /   |
      +---------+---------+                         | stop / rollback      |
                |                                   +----------+-----------+
                v                                              |
      +-------------------+                                     v
      | Domain Services   |                         +----------------------+
      +----+---------+----+                         | Stateful Graph       |
           |         |                              | Orchestrator         |
           v         v                              +----------+-----------+
      +---------+ +---------+                                   |
      | Short   | | Click   |                                   v
      | URLs    | | Events  |                         +----------------------+
      +----+----+ +----+----+                         | Agent Executor Layer |
           |           |                              | deterministic by     |
           +-----+-----+                              | default; LLM seam    |
                 |                                    +----------+-----------+
                 v                                               |
          +--------------+-------------------+                    |
          | PostgreSQL + Flyway + Audit Log  |<-------------------+
          +----------------------------------+

 Observability: Actuator + Micrometer metrics + workflow audit events
 Governance: control token + approval token + node policy + bounded retries
```

## Orchestration graph

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
Requirements -> Codebase Reasoning -> Impact Analysis -> Architecture
                                                       /          \
                                                      v            v
                                             Implementation     Documentation
                                                      \            /
                                                       v          v
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
Requirements -> Ambiguity Review -> HUMAN CLARIFICATION
                                  |
                                  v
                              Architecture
                              /          \
                             v            v
                      Implementation  Documentation
                             \            /
                              v          v
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

The graph is persisted. Node status, attempts, outputs, timestamps and dependency edges are durable, so the workflow is resumable and reviewable.

## SDLC governance model

### Autonomous

Low-risk analysis, architecture, implementation planning, documentation generation, and validation are eligible for autonomous execution.

### Human controlled

Release approval is always a human checkpoint. Ambiguous requirements stop for clarification. Rollback and safe-stop are explicit human-controlled actions. The prototype never mutates cloud infrastructure or deployment credentials autonomously.

### Bounded failure handling

- each executable node has a retry budget
- retries are persisted as attempts
- documentation is the only node with a fallback path in this prototype
- safe-stop blocks outstanding work
- rollback transitions the release outcome to a terminal rolled-back state
- maximum run duration prevents runaway execution

## API surface

### URL shortener

`POST /api/v1/urls`

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

`GET /{code}` redirects to the original URL and records a click event.

`GET /api/v1/urls/{code}/analytics` returns total, last-24-hour and last-7-day clicks.

### Agent control plane

All `/api/v1/agent-runs/**` endpoints require:

```text
X-Agent-Control-Token: local-demo-control-token
```

Create:

```text
POST /api/v1/agent-runs
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

Read run state:

```text
GET /api/v1/agent-runs/{id}
GET /api/v1/agent-runs/{id}/audit
```

## Local setup

### Option A: Docker Compose

```bash
docker compose up --build
```

The application becomes available at `http://localhost:8080`.

Health:

```bash
curl http://localhost:8080/actuator/health
```

### Option B: Run Spring Boot locally

Start PostgreSQL first:

```bash
docker compose up postgres -d
```

Then:

```bash
./mvnw spring-boot:run
```

or use your local Maven installation:

```bash
mvn spring-boot:run
```

## End-to-end demo

The included `scripts/demo.sh` walks through all three assignment scenarios and demonstrates approval, clarification, re-planning, audit data and the URL shortener APIs.

```bash
chmod +x scripts/demo.sh
./scripts/demo.sh
```

## Testing

Default test profile uses H2 for fast, deterministic unit/context tests.

```bash
./mvnw test
```

The dependency set also includes Testcontainers PostgreSQL support so an integration profile can be added without changing the application data-access code. Testcontainers is a standard way to test against the same type of external service used in production instead of relying only on mocks.

## Production evolution path

For an enterprise deployment, the next increments would be:

1. OIDC/JWT authorization for the agent control plane instead of static demo tokens.
2. Kafka-backed domain event publication for click events and orchestration events.
3. Redis cache for hot short-code resolution with cache invalidation on expiry.
4. External durable workflow orchestration if runs need cross-region execution.
5. Real LLM adapters for Claude/OpenAI/Azure OpenAI behind the agent executor interface.
6. Policy-as-code evaluation for security, compliance and change-management rules.
7. OpenTelemetry traces correlated with the persisted workflow/audit identifiers.
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
| Auditability | Immutable-style append-only audit records by application design |
| Reliability metrics | Micrometer counters/timers + Actuator |
| Production code quality | Transactions, validation, persistence, migrations, tests, Docker |
| Final summary | `docs/final-engineering-summary.md` |

## Important design limitation

The prototype intentionally does not claim that an LLM is safe merely because it generated code. Model output is treated as untrusted work product; the orchestrator controls what can run, when it can run, what must stop, and what a human must approve.
