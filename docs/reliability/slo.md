# Service-level objectives

Status: portfolio reliability target, not a claim about historical production performance.

## Scope and indicators

The SLO covers authenticated `/api/v1/**` requests served by RheinOps. Keycloak availability, client-side latency, load-generator latency, health probes, Swagger, and Prometheus scrapes are measured separately and are not included in the application availability SLI.

| Signal | SLI | Objective | Evaluation window |
|---|---|---:|---:|
| Availability | non-5xx API responses / all API responses | 99.9% | rolling 30 days |
| Normal latency | server duration of API responses | p95 < 500 ms; p99 < 1 s | rolling 5 minutes under steady load |
| Spike latency | server duration during the documented 50-VU spike | p95 < 1 s; p99 < 2 s | each spike test |
| Correctness | successful k6 checks | > 99% steady; > 95% spike | each load test |
| Recovery | time from database-path restoration to healthy API traffic | < 5 minutes | each failure drill |

HTTP 4xx responses count as available responses because the server handled the request according to contract. HTTP 5xx responses and transport failures consume the availability budget. Planned local restarts are still visible; a real production service would define maintenance handling with stakeholders rather than silently exclude it.

At 99.9% availability, a 30-day month has an error budget of 43 minutes and 12 seconds. The fast-burn alert fires when five-minute 5xx ratio exceeds 1.44% while traffic is present, which is 14.4 times the monthly budget consumption rate.

## Load-test contract

The normal acceptance scenario is `infra/load/k6/steady-state.js`: ten authenticated support-queue reads per second for three minutes, with up to forty pre-authorised virtual users. The spike scenario is diagnostic rather than a capacity promise and ramps to fifty virtual users.

Before treating a run as evidence:

1. start the full reliability Compose overlay and confirm the API, Keycloak, Prometheus, and Tempo are healthy;
2. use a clean failure state by running `scripts/reliability/recover-database.sh`;
3. record the Git commit, machine resources, scenario, start/end UTC time, and k6 summary;
4. preserve screenshots only after the scenario finishes so transient warm-up is not misrepresented;
5. mark a run failed when any versioned k6 threshold fails.

The repository contains objectives and executable tests, not fabricated benchmark results. Measured results belong in a release artifact produced on a named environment.

## Alert-to-runbook mapping

| Alert | Meaning | First response |
|---|---|---|
| `RheinOpsApiDown` | Prometheus cannot scrape the secured API target | [API unavailable](runbooks/api-unavailable.md) |
| `RheinOpsFastErrorBudgetBurn` | 5xx traffic is rapidly consuming the budget | [High error rate](runbooks/api-high-error-rate.md) |
| `RheinOpsP95LatencyHigh` | p95 is above the normal 500 ms target | [Database latency](runbooks/database-latency.md) |
| `RheinOpsDatabasePoolSaturation` | active database connections exceed 90% of capacity | [Database latency](runbooks/database-latency.md) |
| `RheinOpsSupportSlaBreached` | at least one active case has persisted `BREACHED` escalation | [Support SLA breach](runbooks/support-sla-breach.md) |

## Known limitations

- The lab is single-instance and stores observability data locally; it does not demonstrate high availability.
- Prometheus alert evaluation is configured, but no external notification receiver is committed.
- Tempo uses local storage with 24-hour retention and no authentication; its port is loopback-bound.
- Structured logs remain on container stdout. A production deployment would ship them to a protected central store with retention and access controls.
- The local Keycloak issuer and direct password-grant demo are not production authentication architecture.
