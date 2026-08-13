# Three-minute technical walkthrough

Use the **Start 3-minute walkthrough** button in the [public demo](https://rheinops-portfolio-demo.kadriahmad59.chatgpt.site). The console advances through the same six views below at 30-second intervals. Pause it whenever a reviewer wants to inspect a detail.

## 00:00–00:30 — Operational context

“RheinOps is a Java 21 and Spring Boot service-operations platform. It combines a service catalog, incident management, and SLA-driven customer support. This public console is read-only and uses one versioned fictional scenario, so there are no credentials or customer records. The priority queue shows the central idea: one degraded service can be connected to an incident, an at-risk customer case, and the evidence needed to respond.”

Show: **Operations overview** and the `SUP-0187 → INC-0042 → correlation → trace → runbook` flow.

## 00:30–01:00 — Architecture decision

“The backend is a modular monolith, not a set of microservices. Service catalog, incident, and support are explicit Spring Modulith modules with tested dependency boundaries. That gives one simple deployment and atomic workflows while the project has one owner. The accepted trade-off is one scaling unit and one database boundary; extraction would only be justified by independent scaling or team ownership.”

Show: **Architecture**, the module boundary, PostgreSQL, Keycloak, and observability nodes.

## 01:00–01:30 — Correctness and auditability

“Incident changes are not last-write-wins. The client supplies an expected version for a clear conflict response, and JPA optimistic locking protects the database race. Every real transition appends an actor-attributed timeline row. Java maps that evidence as immutable, and PostgreSQL rejects update and delete operations, so auditability is an enforced invariant rather than a UI convention.”

Show: **Incident timeline**, current version 3, and the two concurrency guards.

## 01:30–02:00 — Support engineering

“A support case snapshots its SLA policy at creation, so later policy changes cannot silently rewrite an existing commitment. Response and resolution deadlines drive on-track, at-risk, breached, or met states. Escalation is monotonic while the case is active. Diagnostics are append-only, cases can link to incidents and runbooks, and management export uses an allow-list so free text and requester data are never selected.”

Show: **Support & SLA**, the breached objective, diagnostics, and redaction note.

## 02:00–02:30 — Reliability evidence

“The reliability lab makes operational claims reproducible. Prometheus and Grafana cover golden signals, Tempo receives W3C HTTP traces, k6 runs versioned normal and spike scenarios, and Toxiproxy injects reversible JDBC latency or outage. Recovery scripts remove the fault without deleting data. The repository also includes alert-linked runbooks, SLO definitions, and example blameless postmortems.”

Show: **Reliability lab**, SLO line, trace waterfall, and inject-observe-recover-learn loop.

## 02:30–03:00 — Delivery evidence

“The engineering story is completed by PostgreSQL integration tests, security-role tests, architecture verification, Maven CI, Compose asset validation, and a production container build. I avoid claims the project cannot prove: the public metrics are fictional snapshots, tracing claims HTTP spans rather than unsupported JDBC child spans, and production limitations are documented. The result is a reviewable backend case study, not just a CRUD demo.”

Return to: **Operations overview** and open the repository link.

## Interview follow-up

Be ready to demonstrate locally:

1. a successful and a stale incident transition;
2. timeline immutability at the API and database layers;
3. the role matrix with two different tokens;
4. SLA escalation and redacted export tests;
5. one database-latency drill and its recovery runbook.

