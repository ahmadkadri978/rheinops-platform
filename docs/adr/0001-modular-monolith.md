# ADR-0001: Start as a modular monolith

- Status: Accepted
- Date: 2026-08-13

## Context

RheinOps needs strong business boundaries and production-oriented behaviour, but its portfolio-scale workload does not justify independent deployments, network failure modes, or duplicated platform infrastructure.

## Decision

Build one Spring Boot deployment and organise capabilities as Spring Modulith modules. A module exposes only types in its root package. Internal web and persistence packages are not integration surfaces. An automated architecture test verifies these rules.

## Consequences

### Positive

- one command starts the complete application;
- local transactions preserve incident and catalog consistency;
- refactoring remains inexpensive while boundaries are executable;
- a future extraction has explicit seams instead of arbitrary package cuts.

### Negative

- modules share one process and database deployment;
- independent scaling and releases are not available;
- developers must actively preserve module ownership inside one repository.

## Revisit when

Reconsider this decision only when measured load, separate team ownership, regulatory isolation, or deployment cadence creates a concrete need for an independently deployed service.
