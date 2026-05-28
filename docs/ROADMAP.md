# Roadmap

## Highest Value

- Move real tests into `app/src/test/java`.
- Add billing regression tests for quantity aggregation, rate recomputation, savings, adjustments, and rounding.
- Add rate resolver tests for fat/SNF slab matching.
- Harden SQLite foreign-key enforcement and migration validation.
- Move runtime data and backups outside the source tree.

## Portfolio Polish

- Add screenshots for the main screens.
- Add an anonymized sample PDF bill.
- Add a short demo video or GIF.
- Add GitHub Actions for compile/test verification.
- Add release packaging notes.

## Product Improvements

- CSV/Excel import for members and daily entries.
- Export monthly billing summaries.
- Operator roles and stronger admin PIN handling.
- Better backup restore status and validation.
- Installer packaging with `jpackage`.

## Engineering Improvements

- Split UI controllers from table/model mapping where screens are large.
- Replace debug `System.out.println` statements with structured logging.
- Add integration tests against a temporary SQLite database.
- Introduce DTOs for report data and billing summaries.
- Document migration policy for released versions.
