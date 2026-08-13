# ADR-0002: Use external OIDC roles and an append-only incident audit log

- Status: Accepted
- Date: 2026-08-13

## Context

Incident data influences operational decisions. RheinOps therefore needs independently issued identities, least-privilege write access, and a history that cannot be silently rewritten through ordinary application or database operations.

## Decision

Run RheinOps as an OAuth2 Resource Server and keep identity lifecycle outside the application. Validate JWT signature, time claims, issuer, and the `rheinops-api` audience. Map only four allow-listed Keycloak realm roles: `VIEWER`, `RESPONDER`, `INCIDENT_MANAGER`, and `ADMINISTRATOR`.

Capture the OIDC subject and preferred username when an incident is created or changes status. Store each action as a new `incident_timeline_events` row in the same transaction as the domain change. Make the JPA entity immutable and reject SQL `UPDATE` or `DELETE` with a PostgreSQL trigger.

Use a bounded, per-instance Bucket4j limit at the API boundary. Do not claim it is a cluster-wide quota.

## Consequences

### Positive

- identity credentials and account lifecycle are not stored in RheinOps;
- forged issuers and tokens intended for another audience are rejected;
- read, response, management, and administrative capabilities are independently demonstrable;
- every meaningful incident transition has an actor and timestamp;
- accidental or application-level audit rewriting fails at the database boundary;
- local Keycloak makes the complete flow reproducible without a cloud dependency.

### Trade-offs

- production availability depends on cached/current signing keys from the identity provider;
- role changes affect future requests but do not rewrite historical actor snapshots;
- the local direct grant is convenient for CLI demonstrations but is not an acceptable production UI flow;
- a database administrator can bypass triggers and therefore remains inside the trusted computing base;
- rate-limit state is local to one application instance.

## Rejected alternatives

- Application-owned passwords: duplicates identity lifecycle and increases credential risk.
- Trusting scopes or every Keycloak role automatically: makes accidental privilege expansion easier.
- A mutable `updated_by` column on incidents: loses the sequence and authorship of earlier actions.
- Logging audit events only: logs may be sampled, rotated, or unavailable to transactional readers.
