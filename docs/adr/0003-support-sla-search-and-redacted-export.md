# ADR-0003: Snapshot SLA commitments and export only allow-listed operational fields

- Status: Accepted
- Date: 2026-08-13

## Context

Support engineers need customer cases, predictable response and resolution targets, searchable diagnostics, and an export suitable for operational review. These features introduce two risks: changing a policy could rewrite the meaning of an existing commitment, and a convenient export could leak requester or diagnostic data.

## Decision

Add a `support` Modulith module for customer organisations, versioned SLA policies, support cases, append-only diagnostics, operational search, and redacted export.

When a case is opened, retain the policy ID and snapshot its calculated response deadline, resolution deadline, and warning threshold on the case. Replacing an active policy deactivates the previous row; it does not alter existing cases. Calculate SLA health from UTC instants and escalate monotonically from `NONE` to `WARNING` to `BREACHED`. A scheduled evaluator refreshes persisted escalation state, and reads perform the same refresh so a stale queue is not returned between scheduled runs.

Use PostgreSQL generated `tsvector` columns and GIN indexes for full-text search across incident text and append-only diagnostic observations. Keep mutable customer contact data out of the search index.

Build CSV exports from an explicit allow-list of operational identifiers, states, deadlines, and timestamps. Do not select requester name/email, free-text case fields, resolution notes, diagnostic text, or actor identity into the export pipeline. Limit exports to 31 days, require a management role, and mark responses `no-store`.

## Consequences

### Positive

- existing support commitments remain explainable after a policy replacement;
- response and resolution risk is visible before and after breach;
- support queues support optimistic concurrency and deterministic lifecycle rules;
- PostgreSQL provides ranked search without introducing a second data store;
- redaction is structural because sensitive columns never enter export generation;
- tests can prove database-level diagnostic immutability and absence of known secrets in CSV output.

### Trade-offs

- targets use elapsed calendar time; business calendars and pause-on-customer-waiting are not implemented;
- SLA refresh performs database writes and increments the case version when escalation changes;
- PostgreSQL full-text configuration is currently English and would need language-aware indexing for multilingual diagnostics;
- CSV is an operational aggregate, not an externally immutable audit archive;
- an administrator with direct database access remains inside the trusted computing base.

## Rejected alternatives

- Resolve deadlines from the current policy on every read: silently changes old customer commitments.
- Export first and redact strings afterwards: sensitive values can leak through new columns or formatting paths.
- Search with unindexed `ILIKE '%query%'`: produces weak relevance and degrades into table scans.
- Add Elasticsearch immediately: adds operational complexity that is not justified by the current scale.
