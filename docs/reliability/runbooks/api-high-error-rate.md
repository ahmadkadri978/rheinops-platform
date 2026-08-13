# Runbook: high API error rate

## Trigger and impact

Use this runbook for `RheinOpsFastErrorBudgetBurn` or a sustained increase in 5xx responses. Writes or reads may be failing even while liveness remains `UP`.

## Diagnose

1. In Grafana, compare 5xx ratio, request rate, p95 latency, and database pool usage over the same window.
2. Group failures by the low-cardinality `uri`, `method`, and `status` metric labels; never add record IDs or user identifiers as labels.
3. Select one failing request's `X-Correlation-ID`, find its structured log entry, and pivot to `traceId` in Tempo.
4. Use the server span duration together with database-pool metrics and the matching safe access/error log; the lab does not pretend that an HTTP span alone identifies a JDBC query.
5. Check whether a Toxiproxy drill is active: `curl -fsS http://localhost:8474/proxies/rheinops-postgres`.

## Recover

- For an active database drill, run `scripts/reliability/recover-database.sh` and continue with the database runbook.
- Reduce test traffic if the spike scenario, rather than normal traffic, is the trigger.
- Roll back only a known recent change with a reviewed rollback path. Do not hide failures by relaxing the alert or returning successful responses for failed work.
- Preserve logs and trace IDs before recreating a crashing process.

## Verify and close

The five-minute 5xx ratio must remain below 0.1%, k6 checks must pass, and no connection-pool waiters should remain for five minutes. Document consumed error budget and create follow-up actions with owners and dates.
