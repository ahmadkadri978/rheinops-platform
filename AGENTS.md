# Repository guidance

## Build and test

- Use Java 21.
- Run `./mvnw test` for fast feedback.
- Run `./mvnw verify` before a pull request; Docker is required for `*IT` tests.
- Keep database changes in additive, versioned Flyway migrations. Never edit a migration already released on `main`.

## Architecture

- Keep business capabilities as top-level Spring Modulith modules under `dev.rheinops.platform`.
- A module may use another module only through types in that module's root package.
- Keep controllers and persistence entities in internal subpackages.
- Store cross-cutting API types in the `shared` module and avoid cyclic dependencies.

## API conventions

- Keep public endpoints under `/api/v1`.
- Validate request bodies and return `ProblemDetail` through `ApiErrorHandler`.
- Propagate `X-Correlation-ID` and never expose stack traces or internal exception messages.
- Use optimistic concurrency for mutable operational records.

## Code review rules

### Incident safety

- Flag any change that bypasses `IncidentEntity.transitionTo` or allows a resolved incident to reopen without a documented decision and test.
- Require concurrency-sensitive updates to verify the expected version and retain the JPA `@Version` guard.

### Data integrity

- Flag JPA schema changes without a matching Flyway migration and integration test.
- Do not log credentials, tokens, full request bodies, or personally identifiable information.

### Module boundaries

- Flag imports from another module's `.internal` or `.web` package. The safe path is a type in the module root package.
