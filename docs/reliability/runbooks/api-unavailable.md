# Runbook: API unavailable

## Trigger and impact

Use this runbook for `RheinOpsApiDown`, failed liveness checks, or connection failures to port 8080. Users may be unable to read or change operational records.

## Diagnose

1. Confirm the symptom without credentials: `curl -fsS http://localhost:8080/actuator/health/liveness`.
2. Inspect container state: `docker compose -f compose.yaml -f compose.reliability.yaml ps`.
3. Inspect the latest API logs: `docker compose -f compose.yaml -f compose.reliability.yaml logs --since=10m api`.
4. Check Prometheus target details at <http://localhost:9090/targets>. A `401` or token error points to Keycloak/service-account configuration, not necessarily an application outage.
5. Check Keycloak and PostgreSQL container health before restarting anything.

Search logs by `correlationId`, `traceId`, exception type, and UTC time. Do not paste bearer tokens or customer fields into tickets or chat.

## Recover

- If a reliability drill is active, run `scripts/reliability/recover-database.sh`.
- If only the API process stopped, recreate that service: `docker compose -f compose.yaml -f compose.reliability.yaml up -d api`.
- If Keycloak is still importing the realm, wait for it to become ready; do not weaken the metrics endpoint to bypass authentication.
- Escalate before touching persistent volumes. Never use `down -v` as a recovery step.

## Verify and close

1. Liveness returns `UP` three times over one minute.
2. Prometheus target returns to `UP` and the alert resolves.
3. An authenticated queue read returns HTTP 200 with `X-Correlation-ID`.
4. Request rate and latency return to baseline for five minutes.
5. Record the timeline, scope, and evidence in a postmortem when the impact exceeded the SLO or required manual recovery.
