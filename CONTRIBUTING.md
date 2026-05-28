# Contributing

This is primarily a personal portfolio project, but the development workflow is intentionally simple.

## Local Checks

Before opening a pull request or pushing a larger change, run:

```bash
./gradlew :app:test
```

## Commit Style

Use short imperative messages:

```text
fix: handle missing saving period
docs: add architecture notes
refactor: simplify billing aggregation
```

## Development Notes

- Keep business rules in `service` classes, not directly in JavaFX views.
- Keep persistent domain state in `entity` classes.
- Add migrations for schema changes instead of relying on Hibernate auto-DDL.
- Avoid committing local/generated runtime data where possible.
- Add tests for billing, rates, savings, and lock behavior when changing those flows.
