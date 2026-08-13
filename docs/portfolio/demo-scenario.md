# Fictional demonstration scenario

## Purpose

Scenario `DEMO-2026-08-13` gives every portfolio screenshot and walkthrough the same operational story. It is deliberately deterministic and non-sensitive. It explains how the implemented backend behaves; it does not impersonate a production environment or claim that the public console writes to the API.

## Seeded snapshot

| Area | Fictional fixture |
|---|---|
| Service | `Checkout API` (`checkout-api`), Tier 1, degraded |
| Incident | `INC-2026-0042`, SEV-2, mitigating, version 3 |
| Support case | `SUP-2026-0187`, P1, in progress, resolution SLA breached by 12 minutes |
| Organisation | `Nordhafen Retail GmbH` |
| Correlation ID | `demo-corr-0042` |
| Trace display | shortened fictional identifier `8f2…a91` |
| Reliability event | reversible database-latency drill followed by recovery |

The overview aggregates six fictional services, two active incidents, and eleven open support cases. These counts exist only to make queue prioritisation visible.

## Storyline

1. A customer report about delayed payment confirmations creates support case `SUP-2026-0187`.
2. Responders link the case to elevated Checkout API latency in incident `INC-2026-0042`.
3. Each incident transition checks the caller-supplied expected version and the JPA version.
4. Actor snapshots and transition details are appended to the incident timeline.
5. The case's snapshotted P1 policy produces first-response and resolution objectives.
6. Correlation data connects the case, incident, access log, HTTP trace, and database-latency runbook.
7. The reliability lab removes the injected latency and records recovery evidence without deleting data.

## Mapping to implemented behaviour

The console deliberately separates presentation from implementation:

- state-machine rules live in the incident and support entities;
- caller and database concurrency guards live in the services and JPA mappings;
- append-only enforcement is implemented in Java and PostgreSQL;
- SLA assessment and monotonic escalation live in the support module;
- identity and route protection live in the shared security boundary;
- SLOs, alert rules, load scenarios, failure scripts, and runbooks are versioned under `infra`, `scripts`, and `docs/reliability`.

No demo fixture is loaded into a public database. A reviewer who wants to execute the real API can start the Docker Compose stack and use the disposable local identities documented in the main README.

