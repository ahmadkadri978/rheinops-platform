# RheinOps Platform v1.0.0

RheinOps v1.0.0 is the first complete portfolio release of the service-operations platform. It combines the work delivered across five milestones into one reviewable Java/Spring Boot case study.

## Highlights

- modular service catalog, incident, and support capabilities with Spring Modulith boundary verification;
- concurrency-safe incident and case lifecycles with append-only actor-attributed evidence;
- Keycloak OIDC/JWT security, explicit operational roles, rate limiting, and secure production defaults;
- snapshotted SLA policies, monotonic escalation, diagnostics, runbook links, PostgreSQL full-text search, and redacted export;
- Prometheus/Grafana/Tempo observability, structured logs, SLOs, k6 scenarios, reversible Toxiproxy drills, runbooks, and postmortems;
- Docker Compose local environment, Testcontainers integration tests, CI quality gates, production image build, and tag-driven release automation;
- public read-only [portfolio console](https://rheinops-portfolio-demo.kadriahmad59.chatgpt.site), versioned fictional scenario, five evidence screenshots, and a three-minute walkthrough.

## Run locally

Requirements: Docker and Docker Compose v2.

```bash
git clone https://github.com/ahmadkadri978/rheinops-platform.git
cd rheinops-platform
git checkout v1.0.0
docker compose up --build
```

Open Swagger UI at <http://localhost:8080/swagger-ui.html>. The disposable local identities and token example are documented in the main README.

## Verification

The release workflow verifies that Maven version `1.0.0` matches tag `v1.0.0`, runs the full Maven verification suite with PostgreSQL Testcontainers, builds the production container, and creates the GitHub release from these notes.

## Deliberate limitations

- The public console is a static, read-only evidence layer with fictional data; it does not expose the backend or a public database.
- Local credentials are disposable demonstrations and are not a production authentication flow.
- Rate limiting is per instance.
- SLA time uses elapsed calendar duration.
- The observability implementation claims HTTP trace propagation, not unverified JDBC child spans.

See the [CV case study](cv-case-study.md) for the architectural trade-offs and the [security threat model](../security/threat-model.md) for the production boundary.

