# Architecture

## Context

RheinOps supports operators who register customer-facing services, manage incidents, and work customer support cases against explicit SLA commitments. The system optimises for a clear domain model, reliable local setup, and operational diagnosis rather than distributed-system complexity.

## System shape

The application is a Spring Modulith modular monolith backed by PostgreSQL. Modules communicate through explicit public APIs in their root packages; persistence entities and web DTOs remain internal.

```mermaid
flowchart TD
    Request[HTTP request] --> Correlation[Correlation filter]
    Correlation --> Limit[Bounded IP rate limit]
    Limit --> JWT[JWT issuer, signature, audience, and role validation]
    JWT --> Controller[Module controller]
    Controller --> Application[Application service]
    Application --> Domain[Domain entity]
    Application --> Repository[JPA repository]
    Repository --> PostgreSQL[(PostgreSQL)]
    Application --> Response[API response]
```

## Module dependency rules

```mermaid
flowchart LR
    Incident[Incident] --> Catalog[Service Catalog]
    Incident --> Shared[Shared Kernel]
    Support[Support Operations] --> Incident
    Support --> Catalog
    Support --> Shared
    Catalog --> Shared
```

- `incident` stores the affected service ID and resolves it through `ServiceCatalog`; it does not import the service catalog's JPA entity.
- Flyway owns the database schema. Hibernate validates the mapping but never mutates production schema.
- Spring Modulith's architecture test detects cycles and illegal access to internal packages.
- `support` validates linked incidents and services through their root-package APIs; it never imports their persistence entities.

## Runtime and observability

- Every HTTP request receives a bounded, validated correlation ID.
- Actuator provides health, liveness, readiness, and Prometheus endpoints.
- PostgreSQL state is retained in a named Compose volume.
- The application shuts down gracefully and disables Open Session in View.
- API requests are rate limited before bearer-token processing; health probes are excluded.

## Consistency model

Incident updates use two guards:

1. the API caller supplies `expectedVersion`, producing a clear conflict response for stale state;
2. JPA `@Version` protects against a race between concurrent transactions.

This is intentionally stricter than last-write-wins because conflicting incident updates can mislead responders.

Support-case updates use the same caller-supplied expected version and JPA `@Version` guard. Creating a case snapshots the selected policy's deadlines and warning threshold, so replacing the policy affects only future cases.

## Support workflow and SLA evaluation

Support cases move through `OPEN`, `IN_PROGRESS`, `WAITING_FOR_CUSTOMER`, `RESOLVED`, and `CLOSED`. Resolution notes are mandatory before resolution or closure. The first real transition records the response time; all targets currently use elapsed calendar time and do not pause while waiting for the customer.

The SLA calculator distinguishes response and resolution objectives and reports `ON_TRACK`, `AT_RISK`, `BREACHED`, or `MET`. Escalation is monotonic (`NONE` → `WARNING` → `BREACHED`), refreshed on a schedule and before support-queue reads, and persisted with an updated optimistic-lock version.

Diagnostics are actor-attributed append-only observations. PostgreSQL generated `tsvector` columns and GIN indexes power ranked full-text search across incident descriptions and diagnostics. Search does not index requester contact data.

Operational CSV export is generated from an allow-list containing references, organisation/service slugs, states, deadlines, and timestamps. Requester data, free text, diagnostics, resolution notes, and actor identity are never selected into the export path. Exports require a management role, are limited to 31 days, and return `Cache-Control: no-store`.

## Security boundary

RheinOps is an OAuth2 Resource Server. It validates JWT signature, expiry, not-before time, exact issuer, and the `rheinops-api` audience. Only the explicit Keycloak realm roles `VIEWER`, `RESPONDER`, `INCIDENT_MANAGER`, and `ADMINISTRATOR` are mapped to authorities; unrelated realm roles are ignored.

The HTTP policy is deny-by-default. Liveness and readiness remain anonymous and contain no component details. Application data requires at least `VIEWER`; incident writes require `RESPONDER`; service registration requires `INCIDENT_MANAGER`; Prometheus requires `ADMINISTRATOR`. Sessions, form login, HTTP Basic, cross-origin support, and multipart uploads are disabled.

Local Compose imports a reproducible Keycloak realm and disposable demo users. Production uses externally supplied issuer and JWK Set addresses, disables API documentation, and expects TLS termination at a trusted ingress. Direct password grants are a local demonstration convenience, not the production authentication flow.

## Audit invariants

Creating an incident appends `INCIDENT_CREATED`; every real status change appends `STATUS_CHANGED`. Each entry stores the identity-provider subject, the display name at action time, the previous and new states, and a UTC timestamp. The incident update and timeline append occur in one database transaction.

The timeline has no update or delete API, its Hibernate mapping is immutable, and PostgreSQL rejects row-level `UPDATE` and `DELETE` through a trigger. A database owner can still disable triggers or rewrite storage; protecting against a compromised database administrator requires external immutable export in a later milestone.

## Rate-limit scope

Bucket4j uses a token bucket keyed by the direct network peer address. Caffeine bounds the number of cached client buckets and expires inactive entries, preventing an attacker from causing an unbounded in-memory map. This implementation is intentionally per-instance. A multi-replica deployment must move the limit to a trusted gateway or a distributed Bucket4j backend before claiming a global quota.
