# Contributing

## Workflow

1. Create a focused branch from `main`.
2. Keep commits small and use an imperative Conventional Commit message.
3. Run `./mvnw test` during development.
4. Run `./mvnw verify` before opening a pull request.
5. Open a draft pull request early and document trade-offs or follow-up work.

## Commit examples

- `feat: add incident acknowledgement`
- `fix: reject stale incident transitions`
- `docs: record authentication decision`
- `chore: update PostgreSQL image`

## Definition of done

- behaviour has focused unit or integration tests;
- API and migration changes are documented;
- logs and errors contain enough operational context without leaking secrets;
- CI passes and the pull request explains both what changed and why.
