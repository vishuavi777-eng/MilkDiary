# Case Study: MilkDiary

## Problem

Small dairy collection outlets often manage daily milk collection, member payments, savings deductions, and monthly reports through spreadsheets or paper registers. That creates repeated manual calculation, weak auditability, and a high chance of mismatch between daily entries and final bills.

MilkDiary was built to centralize those workflows in a desktop application that can run locally without requiring a cloud backend.

## Users

- Dairy outlet operators entering daily collection data
- Outlet owners reviewing member-wise bills and summaries
- Account/admin users applying savings, adjustments, locks, and backups

## Goals

- Reduce repeated spreadsheet work for daily and monthly dairy billing
- Keep data local and easy to back up
- Support Marathi/English usage patterns
- Make rate changes manageable through structured rate plans
- Generate monthly bills and PDFs from the same source of truth

## Solution

The application uses JavaFX for a desktop operator interface, SQLite for local persistence, Hibernate for domain mapping, and Flyway for database migrations. The domain is organized around outlets, members, daily milk entries, rate plans, monthly bills, adjustments, cap locks, and savings periods.

## Engineering Decisions

| Decision | Reason |
| --- | --- |
| JavaFX desktop app | Works well for an offline/local operator workflow |
| SQLite | Simple deployment, no database server required |
| Flyway migrations | Keeps schema evolution explicit |
| Hibernate/JPA | Reduces repetitive persistence code and keeps entities readable |
| Service layer | Keeps UI screens focused on interaction instead of billing rules |
| PDF generation | Produces printable/shareable bills for real-world use |

## Key Workflows

1. Operator selects an outlet and daily session.
2. Member entries are added with quantity, fat, SNF, and species.
3. Rate resolver calculates the applicable rate and amount.
4. Monthly billing aggregates entries and applies savings/adjustments.
5. Bills can be locked to protect historical periods.
6. Reports and backups are generated for operational continuity.

## Business Rules Captured

- Species-specific entries for cow and buffalo milk
- Effective-dated rate plans
- Fat/SNF-based rate matching
- Member-wise monthly bill uniqueness
- Savings deductions per litre
- Bill adjustments and round-off
- Cap/month lock checks

## Result

MilkDiary demonstrates a complete local business application: not just screens, but data modeling, migrations, business rules, report output, backup workflow, and multilingual support.

## Next Improvements

- Add automated regression tests for billing and rate resolution
- Package a production installer
- Move runtime data into a user application directory
- Add screenshots and sample anonymized reports to the portfolio README
- Add import/export workflows for members and entries
