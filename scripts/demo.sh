#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CONTROL_TOKEN="${AGENT_CONTROL_TOKEN:-local-demo-control-token}"
APPROVAL_TOKEN="${APPROVAL_TOKEN:-local-demo-approval-token}"

header=(-H "X-Agent-Control-Token: ${CONTROL_TOKEN}" -H 'Content-Type: application/json')

echo '== Health =='
curl -fsS "${BASE_URL}/actuator/health"; echo; echo

echo '== URL create =='
CREATE=$(curl -fsS -X POST "${BASE_URL}/api/v1/urls" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-001' \
  -d '{"originalUrl":"https://example.com/demo","ttlHours":24}')
echo "$CREATE"; echo
CODE=$(printf '%s' "$CREATE" | python3 -c 'import json,sys; print(json.load(sys.stdin)["code"])')

echo '== Analytics before redirect =='
curl -fsS "${BASE_URL}/api/v1/urls/${CODE}/analytics"; echo; echo

echo '== Greenfield run =='
RUN=$(curl -fsS -X POST "${BASE_URL}/api/v1/agent-runs" "${header[@]}" \
  -d '{"scenario":"GREENFIELD","requirement":"Build a URL shortener service with core APIs, analytics and reliability features."}')
echo "$RUN"; echo
RUN_ID=$(printf '%s' "$RUN" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

echo '== Approve =='
curl -fsS -X POST "${BASE_URL}/api/v1/agent-runs/${RUN_ID}/approve" "${header[@]}" \
  -d "{\"approvalToken\":\"${APPROVAL_TOKEN}\",\"comment\":\"Approved for demo release\"}"; echo

echo '== Final run =='
curl -fsS "${BASE_URL}/api/v1/agent-runs/${RUN_ID}" "${header[@]}"; echo; echo

echo '== Audit =='
curl -fsS "${BASE_URL}/api/v1/agent-runs/${RUN_ID}/audit" "${header[@]}"; echo; echo

echo '== Redirect =='
curl -sS -I "${BASE_URL}/${CODE}"; echo

echo '== Analytics after redirect =='
curl -fsS "${BASE_URL}/api/v1/urls/${CODE}/analytics"; echo; echo

echo 'Demo complete.'
