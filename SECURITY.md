# Security policy

## Reporting a vulnerability

Please do not publish suspected vulnerabilities in a public issue. Use GitHub's private vulnerability reporting feature for this repository when available, or contact the repository owner privately through the contact information on the GitHub profile.

Include the affected version, reproduction steps, impact, and any suggested mitigation. Do not include real credentials, production data, or personal data.

## Supported versions

This project is pre-1.0. Security fixes are applied to the latest revision of `main`.

## Current security scope

RheinOps validates audience-bound OIDC access tokens and applies deny-by-default role-based access. Incident actions are appended to a database-protected audit timeline, and API requests receive a bounded per-instance rate limit.

The Keycloak realm and credentials committed under `infra/keycloak` are disposable local-development fixtures. They are not production secrets and must never be deployed or reused as real credentials. Production must provide its own issuer and JWK Set URLs, terminate TLS at a trusted boundary, and review [the threat model](docs/security/threat-model.md).
