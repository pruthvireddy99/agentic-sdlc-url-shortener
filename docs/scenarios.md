# Required Scenario Demonstrations

## Scenario 1: Greenfield

**Input:**

> Build a URL shortener service with core APIs, analytics and reliability features.

**Decomposition:** requirements -> architecture -> implementation + documentation -> testing -> release readiness -> human approval -> release.

**Validation:**

- requirements node creates normalized acceptance criteria
- architecture node records components and design decisions
- implementation and documentation demonstrate parallel paths
- testing requires both parallel branches before it can execute
- release-readiness records operational checks
- release cannot execute before human approval

**Evidence to show during the interview:** node status graph, parallel timestamps, audit events and approval transition.

## Scenario 2: Brownfield

**Input:**

> Add analytics to an existing URL shortener without breaking redirect behavior.

**Decomposition:** requirements -> codebase reasoning -> impact analysis -> architecture -> implementation + documentation -> testing -> release readiness -> approval -> release.

**Codebase reasoning output covers:** API boundary, persistence modules, analytics flow, orchestration and observability.

**Validation:** testing explicitly includes redirect correctness, idempotency and regression focus.

## Scenario 3: Ambiguous

**Input:**

> Make the URL shortener production ready and use an AI agent to deliver it.

The ambiguity review identifies missing choices such as retention, deployment identity and external LLM contract.

The run stops at `AWAITING_CLARIFICATION`.

Human response:

> Use 90-day analytics retention and treat the LLM provider as an optional adapter.

That clarification is added to context and decision lineage. The plan version increments and the graph resumes.

The run later stops again at `AWAITING_APPROVAL` before release.

## Re-planning demonstration

At any non-terminal point, call `POST /api/v1/agent-runs/{id}/replan` with a new assumption. The plan version increases and downstream work is invalidated so stale results are not silently reused.
