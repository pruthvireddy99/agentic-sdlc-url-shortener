# Interview Demonstration Talk Track

## Opening

> I intentionally separated the product plane from the AI control plane. The URL shortener proves normal senior Java engineering; the orchestration plane proves controlled agentic SDLC execution.

## 1. Requirement understanding

Show a run immediately after creation.

Point out:

- normalized problem
- acceptance criteria
- ambiguities
- decision lineage

Say:

> I do not let a model silently turn ambiguity into an implementation decision. Ambiguity becomes state, and state can require human clarification.

## 2. Task decomposition

Show the node graph.

Say:

> These are persisted dependency edges. The graph is not just documentation; the executor uses it to decide what is eligible to run.

## 3. Non-linear execution

Show the greenfield run after architecture.

Say:

> Implementation and documentation are siblings. They can execute concurrently. Testing is a synchronization barrier because it consumes both outputs.

## 4. Governance

When the run reaches `AWAITING_APPROVAL`:

Say:

> Release is a high-impact action, so the graph physically cannot advance until the human approval node is successful.

Approve it and show the state transition in the audit log.

## 5. Ambiguity

Run the ambiguous scenario.

Say:

> The system stops instead of inventing a deployment or retention policy. A human supplies the missing decision, the context is updated, the plan version increases, and downstream work is recalculated.

## 6. Brownfield

Run the brownfield scenario.

Say:

> Brownfield work has different reasoning stages. I explicitly model codebase reasoning and impact analysis before architecture so the implementation agent does not jump directly from a ticket to code changes.

## 7. Reliability

Point to:

- retry budgets
- safe stop
- rollback endpoint
- documentation-only fallback
- max run duration

Say:

> Controlled autonomy means the system has bounded failure behavior. There is no infinite retry loop and no autonomous infrastructure mutation.

## 8. Why deterministic agents?

Say:

> For a take-home evaluation, deterministic handlers make the orchestration itself reproducible. The executor boundary is where I would plug in Claude, OpenAI or an internal model gateway. The governance semantics do not change because the model changes.

## 9. Senior-level closing

> The important engineering decision is to treat the model as an untrusted worker inside a controlled lifecycle. Agents can propose and execute bounded work, but the system owns state, dependency gates, validation, change control, auditability and human approval.
