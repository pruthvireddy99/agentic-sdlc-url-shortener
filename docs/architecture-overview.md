# Architecture Overview

## 1. Problem statement

Transform a natural-language software requirement into a reviewable engineering outcome using controlled agentic execution.

The prototype has two planes:

- **Product plane:** a URL shortener with creation, redirect, analytics and reliability behavior.
- **Control plane:** a stateful SDLC orchestration engine that decomposes requirements, coordinates agents, validates outputs and enforces governance.

## 2. Core design principles

### Explicit state over prompt history

All meaningful orchestration state lives in PostgreSQL. A run can therefore be queried after the process exits, reviewed by another person, and resumed without trusting model conversation history as the source of truth.

### Graph over chain

A workflow is a directed acyclic dependency graph. Independent nodes can run in parallel. Downstream nodes start only when all required dependencies satisfy their success condition.

### Policy over capability

An agent may be technically capable of performing an action without being authorized to perform it autonomously. Authorization is decided by the control plane.

### Outputs become typed workflow context

Every successful node produces structured JSON output. The orchestrator stores that output under the node key and appends a decision to the lineage. Subsequent agents receive the accumulated context.

### Human approval is a state, not a comment

Approval is persisted as a node transition. A downstream release cannot run until the approval node is successful.

## 3. Persistence model

`orchestration_runs`

- scenario
- current status
- plan version
- original requirement
- cross-stage context
- lifecycle timestamps
- stop reason

`orchestration_nodes`

- node key and type
- dependency list
- status
- attempts / maximum attempts
- structured output
- error details
- timing

`audit_events`

- event type
- actor
- node
- event timestamp
- payload

This separation allows operational queries such as “show all failed validation nodes for brownfield runs” without parsing prompts.

## 4. Concurrency

When multiple nodes are ready, the executor submits them to a bounded thread pool. Their work uses the same immutable snapshot of the current workflow context. Outputs are merged after all sibling futures complete. This creates an explicit synchronization barrier before the next graph wave.

The first intentional parallel branch is implementation + documentation after architecture.

## 5. Re-planning

A re-plan increments `planVersion`, records the reason and changed assumption in decision lineage, invalidates downstream work, and reruns the affected graph portion under the new context.

The run therefore retains:

- original requirement
- previous decisions
- re-plan reason
- new assumptions
- plan version
- newly generated downstream outputs

## 6. Governance controls

### Entry gates

Every graph begins with requirement normalization. Ambiguous runs may stop at the ambiguity review gate.

### Exit gate

Release readiness must succeed before the human approval checkpoint.

### High-impact action

Release remains behind the approval node.

### Safe stop

The stop operation moves the run to a terminal state and blocks outstanding work. This is designed as a fail-closed behavior.

### Rollback

Rollback marks release-related successful nodes as rolled back and transitions the run to `ROLLED_BACK`. In a real deployment, this state transition would be connected to an approved deployment rollback action.

### Bounded retries

Each node receives a retry budget. The graph never retries indefinitely.

### Fallback

The prototype only permits fallback for documentation because dropping a documentation enhancement is lower impact than inventing or silently skipping implementation or test work.

## 7. Observability

Application metrics are exposed through Actuator. The orchestration layer records counters/timers for node success/failure, run latency, safe stops, fallbacks and rollbacks.

The persisted audit event stream provides business-level traceability that complements infrastructure telemetry.

## 8. Security posture

The shortener is intentionally public-facing. The agent control plane is separately protected by a control token in the prototype. Approval requires a separate approval token.

This is a demonstration guardrail, not a production identity solution. The production evolution path is OIDC/JWT + least privilege + policy-as-code.
