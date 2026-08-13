# Delivery roadmap

The roadmap is ordered to produce reviewable evidence for both junior software engineering and support engineering roles.

## Milestone 1 — Foundation and incident core

- [x] Java/Spring Boot project with reproducible Maven wrapper
- [x] modular service catalog and incident management boundaries
- [x] PostgreSQL schema managed by Flyway
- [x] validated REST API and consistent problem details
- [x] incident lifecycle and optimistic concurrency
- [x] Docker/Compose local environment
- [x] unit, architecture, and PostgreSQL integration tests
- [x] CI build, coverage report, and container build

## Milestone 2 — Security and auditability

- [ ] OIDC/JWT resource server with a local Keycloak profile
- [ ] roles for viewer, responder, incident manager, and administrator
- [ ] immutable incident timeline with actor and timestamp
- [ ] security integration tests and threat model
- [ ] API rate limiting and secure production defaults

## Milestone 3 — Support workflows

- [ ] customer organisations and support cases
- [ ] SLA policies, breach prediction, and escalation rules
- [ ] runbook links and resolution notes
- [ ] full-text search across incidents and diagnostics
- [ ] operational export with redaction rules

## Milestone 4 — Reliability lab

- [ ] Prometheus alert rules and Grafana dashboards
- [ ] structured logs and trace propagation
- [ ] load-test scenarios with documented SLOs
- [ ] deliberate database/network failures and recovery runbooks
- [ ] postmortem template and at least two example postmortems

## Milestone 5 — Portfolio release

- [ ] deployed demonstration environment with seeded non-sensitive data
- [ ] screenshots and a three-minute technical walkthrough
- [ ] architecture diagram and trade-off discussion in the CV case study
- [ ] tagged `v1.0.0` release with release notes
