# Local Runbook

## Start

```bash
docker compose up --build
```

## Stop

```bash
docker compose down
```

To remove persistent PostgreSQL data as well:

```bash
docker compose down -v
```

## Health checks

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/demo
```

## Logs

```bash
docker compose logs -f app
```

## Troubleshooting

### Port 5432 is occupied

Change the host port in `docker-compose.yml` and set `DB_URL` accordingly for a local app run.

### Port 8080 is occupied

Change the app port and `PUBLIC_BASE_URL` together.

### Approval returns 401/403

Verify both the control header and approval token. The control token is `local-demo-control-token` by default; the approval token is `local-demo-approval-token`.

### A run is stopped unexpectedly

Inspect `/api/v1/agent-runs/{id}/audit` and the Actuator metrics. A run can stop because of retry exhaustion, the configured maximum duration, safe-stop, or rollback.
