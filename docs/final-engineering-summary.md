# Final Engineering Summary

## Plan and rationale

The prototype is designed around a durable orchestration state machine because the assignment's differentiator is controlled autonomy across the SDLC rather than code generation alone.

The product is a realistic but bounded URL shortener: URL creation, collision-safe short codes, idempotency, redirects, analytics, expiration, database migration and observability. The control plane then treats requirement work as a graph of engineering stages.

## Key decisions

1. **Java 21 + Spring Boot** to align with the candidate's strongest backend skill set and enterprise role requirements.
2. **PostgreSQL + Flyway** for durable state and explicit schema evolution.
3. **Persisted graph nodes** instead of only in-memory queues, allowing reviewable state and recovery.
4. **Parallel execution** for independent implementation/documentation paths with a testing synchronization barrier.
5. **Human approval node** before release, making oversight enforceable.
6. **Deterministic agent handlers** for reproducible evaluation, with a model-provider seam for real LLM execution.
7. **Fail-closed controls**: bounded retries, safe stop, rollback and a tightly scoped fallback path.

## Risks and trade-offs

### LLM output correctness

Risk: generated code/design may be incorrect or insecure.

Control: agent output is treated as untrusted; testing, release-readiness and human approval remain mandatory for high-impact work.

### Prompt/context drift

Risk: later stages may act on obsolete upstream assumptions.

Control: structured context, decision lineage and explicit re-planning with plan versioning.

### Orchestration complexity

Risk: a workflow engine can become harder to maintain than the application itself.

Control: explicit node definitions, small state model and a narrow number of governance primitives.

### Static control tokens

Risk: demo tokens are not suitable for production.

Control: keep tokens configurable through environment variables and document migration to OIDC/JWT.

### Analytics write amplification

Risk: synchronous click-event persistence can become expensive at very high scale.

Control: current design keeps data correctness simple; next scale step is Kafka/event ingestion plus asynchronous aggregation.

### URL redirect security

Risk: URL shorteners are intentionally redirect-oriented and can be abused for phishing or malicious destinations.

Control: only HTTP/HTTPS URLs are accepted; future production controls should include reputation/allow-list integration where required by business policy.

## Validation approach

- input validation at REST boundaries
- transactional writes for URL creation and orchestration state changes
- database uniqueness constraints for short-code and idempotency correctness
- bounded retry logic
- dependency gating
- approval gating
- safe-stop tests
- replan path
- audit assertions

## Assumptions

- the assignment evaluator can run Docker or a local PostgreSQL instance
- no browser UI is required by the deliverables
- LLM provider credentials are not required for the prototype evaluation
- deployment infrastructure mutation is out of scope for the take-home prototype

## Limitations

This is a reviewable prototype, not a production deployment platform. A real Schwab environment would add enterprise identity, centralized policy-as-code, Kafka/event streaming, distributed tracing, secrets management, deployment controllers and a durable external workflow engine.

## Interview talking point

The strongest design point is not “the AI can write code.” The strong point is that the engineering system treats agents as bounded workers inside a governed lifecycle: they can execute, but they cannot self-authorize release, bypass dependency gates, retry forever, or silently continue after a material requirement change.


## Custom orchestration metrics

The service exports orchestration-specific Micrometer metrics in addition to standard JVM, HTTP, JDBC, and executor metrics. These include run starts/completions/failures, node retries, node recovery latency, approvals, replans, safe stops, fallbacks, rollbacks, node latency, and end-to-end run latency. Prometheus scraping is exposed through `/actuator/prometheus`.

For this prototype, recovery latency is measured for an in-process failed-node-to-successful-retry path. A production implementation should persist incident/recovery timestamps if MTTR must survive process restarts.
