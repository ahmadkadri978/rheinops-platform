# Runbook: database latency or pool saturation

## Trigger and impact

Use this runbook for `RheinOpsP95LatencyHigh`, `RheinOpsDatabasePoolSaturation`, database timeouts, or the latency drill. Requests may queue, breach latency SLOs, and eventually return 5xx.

## Diagnose

1. Compare `hikaricp_connections_active`, `pending`, and `max` with HTTP p95/p99.
2. Check application logs for connection acquisition or JDBC timeouts using UTC time and trace ID.
3. Inspect the slow server trace in Tempo, then correlate its time window with pool metrics and database error logs. The lab does not add a synthetic JDBC child span.
4. For the local lab, inspect proxy state: `curl -fsS http://localhost:8474/proxies/rheinops-postgres`.
5. Confirm whether the latency toxic was intentionally injected before changing configuration.

Do not increase the pool blindly. More connections can move contention into PostgreSQL and worsen recovery.

## Recover

For the controlled lab:

```bash
scripts/reliability/recover-database.sh
```

For a real environment, stop or reduce non-essential load, identify the slow query or infrastructure dependency, and coordinate database changes with the database owner. Preserve query plans and timings without copying requester data.

## Verify and close

1. Proxy is enabled and has no `database-latency` toxic.
2. Pending connections return to zero and active connections remain below 70% for five minutes.
3. p95 returns below 500 ms and steady-state k6 thresholds pass.
4. No support-case or incident write was reported successful without being committed.
5. Capture cause, error-budget impact, and preventative action in a postmortem.
