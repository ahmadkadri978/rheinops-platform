# ADR-0004: Build a secured, reproducible reliability lab

- Status: accepted
- Date: 2026-08-13

## Context

RheinOps already exposed Actuator metrics and request correlation IDs, but those features did not demonstrate alerting, diagnosis, trace propagation, performance objectives, or safe recovery. A portfolio project must make reliability claims executable without presenting a local single-instance setup as production-grade infrastructure.

The metrics endpoint is administrator-only. Making it anonymous for Prometheus would weaken the security boundary established in Milestone 2. Failure experiments must also be isolated from the normal developer stack and reversible without deleting state.

## Decision

Add a separate `compose.reliability.yaml` overlay with:

- Prometheus using an OAuth2 client-credentials Keycloak service account with `ADMINISTRATOR` role;
- a provisioned Grafana dashboard for traffic, errors, latency, database pool pressure, and low-cardinality support SLA state;
- OpenTelemetry/Micrometer tracing with W3C propagation and OTLP export to a local single-binary Tempo instance;
- ECS JSON console logs so `correlationId`, `traceId`, and `spanId` remain machine-readable without a custom logging stack;
- Toxiproxy only on the reliability database path, plus explicit injection and recovery scripts;
- versioned k6 steady-state and spike scenarios tied to documented SLOs;
- alert-to-runbook links and blameless postmortem artifacts.

Tracing export is disabled in the default profile, sampled at 10% by default, and enabled at 100% only in the local reliability profile. The production profile emits ECS JSON but requires an explicit OTLP endpoint/enablement decision. Metrics use bounded status/escalation labels and never use record IDs, actors, requester data, or correlation IDs as labels.

## Consequences

The repository now has an executable diagnosis path from SLI to dashboard, log correlation, trace, runbook, recovery, and postmortem. The secured scrape preserves least privilege better than exposing metrics anonymously. The overlay keeps normal local development smaller and prevents accidental chaos injection into the base database route.

The lab adds several containers and local-only disposable secrets. It remains single-node, has no external alert delivery, and does not collect logs centrally. Tempo and Grafana are loopback-bound local tools, not hardened production deployments. These limitations are documented rather than hidden.

## Alternatives considered

- **Expose Prometheus anonymously:** rejected because monitoring convenience does not justify weakening an established administrator boundary.
- **Use a shared administrator password grant:** rejected; a dedicated service identity is auditable and carries only the required role.
- **Add Loki:** deferred. ECS stdout proves structured log design without expanding the milestone with a second retention/query subsystem.
- **Inject failures by stopping PostgreSQL:** rejected for routine drills because it mixes process failure with network failure and encourages unnecessarily broad recovery actions.
- **Claim benchmark numbers in documentation:** rejected. Thresholds are versioned; measured evidence must name the commit and environment where it was produced.
