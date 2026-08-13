# Lab postmortem: injected database latency exhausted request capacity

> This is a fictional, reproducible reliability-lab example. It is not a production incident or a claim about real users.

Status: reviewed example  
Incident window (UTC): 2026-08-13 10:00 – 10:08  
Severity: simulated SEV-2  
Author: portfolio maintainer

## Executive summary

During the documented spike scenario, the database latency drill added 1.5 seconds plus jitter to every downstream response. Requests accumulated behind the ten-connection Hikari pool, p95 exceeded the objective, and some requests failed after waiting for a connection. Removing the toxic restored normal latency without restarting the API or database.

## Impact

- Simulated authenticated support-queue reads became slow and then intermittently returned 5xx.
- The normal p95 target of 500 ms was breached during the exercise.
- No production users, personal data, or external systems were involved.
- The drill used read requests only; persisted business state was unchanged.

## Detection

Grafana showed p95 rising with active/pending database connections. `RheinOpsP95LatencyHigh` was designed with a ten-minute `for` window and therefore did not page during this intentionally short drill; this was expected. The server trace duration, pool metrics, proxy state, and matching structured log jointly isolated the database path; no unsupported JDBC-child-span claim was made.

## Timeline (UTC)

| Time | Event, decision, or evidence |
|---|---|
| 10:00 | Spike test started and baseline checks passed. |
| 10:01 | `inject-db-latency.sh` added 1500 ms latency and 200 ms jitter. |
| 10:02 | p95 crossed 500 ms; pool active connections approached maximum. |
| 10:03 | Pending connections and 5xx responses appeared. |
| 10:04 | Server traces aligned with saturated pool metrics and the active proxy toxic; no CPU or JVM pressure was present. |
| 10:05 | Operator followed the database-latency runbook and ran recovery. |
| 10:06 | Pending connections returned to zero. |
| 10:08 | Five consecutive checks passed and the exercise ended. |

## Root cause and contributing factors

The direct cause was the deliberate downstream latency toxic. The spike created arrival concurrency faster than the fixed-size pool could complete work. Connection acquisition timeout bounded the failure but could not preserve latency once all connections were occupied.

Contributing factors were a single application instance, no admission control based on pool pressure, and a deliberately aggressive spike. Increasing the pool was rejected as an immediate response because it could transfer overload into PostgreSQL.

## Resolution and recovery

`recover-database.sh` deleted the latency toxic and re-enabled the proxy. The team verified proxy state, pool waiters, p95, 5xx ratio, and successful authenticated reads. Neither application nor database restart was required.

## Five whys

1. Why were requests slow? Database responses were intentionally delayed.
2. Why did some requests fail? All connections were busy longer than new requests could wait.
3. Why was latency not contained? The single instance had no dependency-aware admission control.
4. Why did the alert not fire during the first minutes? Its ten-minute duration intentionally filters short transients.
5. Why run the exercise? To verify that metrics, traces, recovery tooling, and operator decisions agree before a real failure.

## What went well

- Metrics, trace spans, and structured logs pointed to the same dependency.
- Recovery was reversible and did not delete or restart stateful services.
- The runbook cautioned against blindly increasing pool size.

## What did not go well

- The dashboard did not initially state that the p95 alert had a ten-minute pending period.
- No automated drill report captured start/end state.

## Where we got lucky

- The exercise used a read-only workload, so ambiguous retry semantics did not affect writes.

## Corrective actions

| Action | Type | Owner | Due | Verification | Status |
|---|---|---|---|---|---|
| Document the alert pending period in the SLO map | learn | portfolio maintainer | Milestone 4 | reviewer reads alert and SLO together | done |
| Add a write-path retry/idempotency exercise | prevent | portfolio maintainer | Milestone 5 backlog | integration and k6 scenario | planned |
| Preserve a machine-readable k6 summary in release evidence | detect | portfolio maintainer | Milestone 5 | release artifact exists | planned |

## Lessons and follow-up

Pool saturation is an effect, not proof that the pool is undersized. The first safe response is to locate downstream time, reduce pressure, and restore the dependency. Capacity tuning requires database evidence and a repeatable test.
