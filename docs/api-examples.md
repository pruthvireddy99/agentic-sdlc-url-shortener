# API Examples

## 1. Create a URL

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-001' \
  -d '{"originalUrl":"https://www.schwab.com","ttlHours":24}' \
  http://localhost:8080/api/v1/urls
```

## 2. Resolve the URL

```bash
curl -I http://localhost:8080/<code>
```

## 3. Read analytics

```bash
curl http://localhost:8080/api/v1/urls/<code>/analytics
```

## 4. Start a greenfield run

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -H 'X-Agent-Control-Token: local-demo-control-token' \
  -d '{"scenario":"GREENFIELD","requirement":"Build a URL shortener service with core APIs, analytics and reliability features."}' \
  http://localhost:8080/api/v1/agent-runs
```

The returned status should reach `AWAITING_APPROVAL` before release.

## 5. Approve the run

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -H 'X-Agent-Control-Token: local-demo-control-token' \
  -d '{"approvalToken":"local-demo-approval-token","comment":"Approved for prototype release"}' \
  http://localhost:8080/api/v1/agent-runs/<run-id>/approve
```

## 6. Ambiguous run

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -H 'X-Agent-Control-Token: local-demo-control-token' \
  -d '{"scenario":"AMBIGUOUS","requirement":"Make the URL shortener production ready and use an AI agent to deliver it."}' \
  http://localhost:8080/api/v1/agent-runs
```

Then clarify:

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -H 'X-Agent-Control-Token: local-demo-control-token' \
  -d '{"clarification":"Use 90-day analytics retention and treat the LLM provider as an optional adapter."}' \
  http://localhost:8080/api/v1/agent-runs/<run-id>/clarify
```

## 7. Replan

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -H 'X-Agent-Control-Token: local-demo-control-token' \
  -d '{"reason":"Deployment target changed","changedAssumption":"Target Kubernetes instead of PCF"}' \
  http://localhost:8080/api/v1/agent-runs/<run-id>/replan
```

## 8. Safe-stop

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -H 'X-Agent-Control-Token: local-demo-control-token' \
  -d '{"reason":"Security review identified a release-blocking issue"}' \
  http://localhost:8080/api/v1/agent-runs/<run-id>/stop
```

## 9. Audit trail

```bash
curl \
  -H 'X-Agent-Control-Token: local-demo-control-token' \
  http://localhost:8080/api/v1/agent-runs/<run-id>/audit
```
