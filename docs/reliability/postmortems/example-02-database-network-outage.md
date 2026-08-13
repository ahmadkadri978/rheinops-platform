# Lab postmortem: database network path disabled

> This is a fictional, reproducible reliability-lab example. It is not a production incident or a claim about real users.

Status: reviewed example  
Incident window (UTC): 2026-08-13 11:00 – 11:07  
Severity: simulated SEV-1  
Author: portfolio maintainer

## Executive summary

The database-outage drill disabled the Toxiproxy route between RheinOps and PostgreSQL while steady authenticated traffic continued. Business requests failed even though process liveness and the Prometheus scrape remained available. Re-enabling the route restored traffic; no volume, container, or schema change was required.

## Impact

- Simulated support-queue reads returned 5xx while the proxy was disabled.
- Liveness stayed `UP`, demonstrating that process health is not dependency availability.
- No external user, production data, or real customer was involved.
- There was no data loss because the exercise used reads and did not terminate PostgreSQL.

## Detection

The 5xx ratio rose immediately under steady traffic and consumed the simulated error budget. Grafana showed a healthy scrape alongside failing business traffic. The server traces aligned with structured connection-error logs; querying Toxiproxy showed `enabled: false`.

## Timeline (UTC)

| Time | Event, decision, or evidence |
|---|---|
| 11:00 | Steady-state test began with passing thresholds. |
| 11:01 | `inject-db-outage.sh` disabled the database proxy. |
| 11:01 | Business reads began failing; liveness stayed healthy. |
| 11:02 | Operator correlated 5xx metrics, a request trace, and proxy state. |
| 11:03 | Database-unavailable runbook selected; PostgreSQL restart was explicitly avoided. |
| 11:04 | `recover-database.sh` re-enabled the route. |
| 11:05 | Successful requests resumed and pool waiters drained. |
| 11:07 | Recovery checks passed for two minutes and the drill closed. |

## Root cause and contributing factors

The direct cause was the deliberate disabling of the only database network route. RheinOps is a single-instance modular monolith with one PostgreSQL dependency; it cannot serve database-backed reads when that dependency is unreachable.

The lab intentionally has no replica, cache-based degraded read mode, or automated circuit breaker. Liveness correctly described the process but was insufficient as a user-impact signal.

## Resolution and recovery

The operator used the proxy API through `recover-database.sh`, verified the route definition, then checked authenticated API requests, pool state, p95, and alert recovery. PostgreSQL and the application remained running.

## Five whys

1. Why did API reads fail? They required an unreachable PostgreSQL connection.
2. Why was PostgreSQL unreachable? The only proxy route was intentionally disabled.
3. Why was there no degraded response? Current domain reads have no safe cached source of truth.
4. Why did liveness remain green? It checks process viability, not every dependency or user journey.
5. Why could this mislead an operator? A single health indicator cannot replace traffic-based SLIs and dependency evidence.

## What went well

- The failure was isolated to the reliability overlay and reversible in one command.
- The operator did not restart healthy stateful infrastructure.
- Traffic metrics detected user impact even though liveness remained green.

## What did not go well

- The lab has no synthetic authenticated readiness probe.
- Write retry and ambiguous commit behaviour were not exercised.

## Where we got lucky

- The proxy control plane stayed reachable and allowed immediate recovery.

## Corrective actions

| Action | Type | Owner | Due | Verification | Status |
|---|---|---|---|---|---|
| Document the difference between liveness and business availability | learn | portfolio maintainer | Milestone 4 | runbook and example reviewed | done |
| Add a synthetic authenticated queue-read signal | detect | portfolio maintainer | future reliability backlog | alert fires when the journey fails | planned |
| Define idempotency policy before adding automated write retries | prevent | portfolio maintainer | Milestone 5 backlog | ADR and integration tests | planned |

## Lessons and follow-up

“The process is alive” and “users can complete work” are different claims. Operations decisions should combine traffic SLIs, dependency telemetry, traces, and a narrowly scoped recovery action.
