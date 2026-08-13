# RheinOps threat model

## Scope and assumptions

This model covers the Spring Boot API, PostgreSQL data, the OIDC trust relationship, and the local Compose boundary. It assumes production TLS terminates at a trusted ingress, the identity provider protects signing keys, and database administrator access is tightly controlled. The committed Keycloak realm is a development fixture, not a production identity deployment.

## Assets

- service and incident availability data;
- incident state and optimistic-concurrency version;
- actor-attributed incident timeline;
- customer contacts, support-case text, and diagnostic observations;
- SLA commitments, escalation state, and redacted operational exports;
- OIDC signing-key trust configuration;
- operational health and metrics;
- database and identity-provider credentials.

## Trust boundaries

1. Untrusted client to the HTTP API.
2. API to the external OIDC provider's JWK Set.
3. API to PostgreSQL.
4. Trusted reverse proxy to the application network peer address.
5. Operator access to Keycloak and database administration.

## Threats and controls

| Threat | Primary controls | Residual risk / next control |
|---|---|---|
| Forged or altered bearer token | Asymmetric signature validation through the configured JWK Set | Compromised identity-provider signing key remains authoritative |
| Token issued by an attacker-controlled or unrelated provider | Exact issuer validation | Configuration changes are privileged deployment operations |
| Valid token intended for another API | Required `rheinops-api` audience | Audience configuration must match the production client mapper |
| Privilege escalation through unrelated realm roles | Four-role allow-list and deny-by-default routes | Identity administrators can intentionally assign a privileged allowed role |
| Anonymous read or write | Stateless bearer authentication on application endpoints | Public liveness/readiness intentionally reveal only `UP`/`DOWN` |
| Viewer changes operational state | Explicit method-and-path role matrix; integration test expects `403` | Future endpoints must be added to the matrix before release |
| Concurrent or replayed incident update | Caller-supplied expected version plus JPA `@Version` | A replay with the current version remains a valid authorised action |
| Audit row modified or deleted by application code | No mutation API, immutable Hibernate entity, PostgreSQL mutation trigger | Database owners can disable triggers; export to external immutable storage is future work |
| Diagnostic evidence rewritten | Append-only API, immutable Hibernate entity, PostgreSQL mutation trigger | Database owners remain able to bypass the trigger |
| Customer PII leaked through operational export | Management-only endpoint; explicit non-PII column allow-list; 31-day window; `no-store` response | Export recipients and downloaded files require their own access and retention controls |
| Sensitive data exposed through search | Search indexes incidents and diagnostics only; requester contact fields are excluded; authenticated read role required | Operators must not paste credentials or tokens into diagnostic notes |
| SLA policy change rewrites an existing commitment | Deadlines and warning threshold are snapshotted when the case opens; old policies are retained inactive | Calendar/business-hour policies remain future work |
| Actor renamed or removed in the identity provider | Subject and display-name snapshot stored with each event | The display name is historical data and may require retention handling |
| Request flood | Token bucket, bounded client cache, inactive-client expiry | Limit is per instance; use a trusted gateway/distributed backend for replicas |
| Spoofed forwarding header bypasses IP quota | Limit keys use the direct peer address and forwarded headers are disabled | A production proxy must be explicitly trusted and tested before enabling forwarding |
| Sensitive diagnostic exposure | No stack traces/messages, health details disabled, metrics administrator-only, OpenAPI disabled in `prod` | Application logs still require access control and retention policy |
| Browser cross-site request abuse | Stateless bearer tokens, CORS disabled, CSRF disabled only for the non-cookie API | A future cookie-based client requires a separate CSRF design |
| Oversized or unexpected upload | Bounded request headers and multipart disabled | A trusted ingress should also enforce body and connection limits |

## Security verification

- unit tests verify role-claim allow-listing and rate-limit rejection;
- integration tests verify anonymous `401`, viewer write/export `403`, permitted role workflows, actor attribution, PostgreSQL audit/diagnostic mutation rejection, full-text search, SLA escalation, and export redaction;
- CI runs migrations against real PostgreSQL and builds the production container;
- dependency and container updates are proposed by Dependabot.

## Deployment checklist

- use a production OIDC realm and short-lived access tokens;
- use Authorization Code with PKCE for interactive clients;
- provide explicit `RHEINOPS_JWT_ISSUER_URI` and `RHEINOPS_JWT_JWK_SET_URI`;
- terminate TLS at a trusted ingress and keep application/database ports private;
- replace the per-instance rate limit before scaling horizontally;
- rotate database and identity credentials outside source control;
- protect metrics, logs, backups, and audit exports with least privilege;
- test backup restoration and identity-provider signing-key rotation.
