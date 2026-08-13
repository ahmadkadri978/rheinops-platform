# Runbook: database network unavailable

## Trigger and impact

Use this runbook when authenticated application requests fail because PostgreSQL cannot be reached. Liveness can remain `UP`; readiness and business requests provide stronger evidence of dependency failure.

## Diagnose

1. Confirm business-request failures and their correlation IDs.
2. Inspect API logs and Tempo traces for connection refusal, timeout, or pool acquisition failure.
3. Check PostgreSQL health and the Toxiproxy definition independently.
4. In the lab, query `http://localhost:8474/proxies/rheinops-postgres` and check `enabled`.
5. Distinguish network-path failure from bad credentials or a failed Flyway migration.

## Recover

For the controlled lab, re-enable the proxy and clear toxics:

```bash
scripts/reliability/recover-database.sh
```

Do not restart PostgreSQL when only the proxy is disabled, and never delete the volume. In production, restore routing/firewall/service discovery through the authorised platform process; do not bypass network controls ad hoc.

## Verify and close

Run an authenticated read and a reversible test write through the public API. Confirm 5xx ratio, pool waiters, p95, and Prometheus alerts recover. Reconcile any ambiguous client retries using idempotency and persisted state rather than assuming the write failed.
