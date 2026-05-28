# Architecture

MilkDiary is a local-first JavaFX desktop application. It keeps the operator workflow simple while isolating persistence, billing, and reporting concerns into separate packages.

## Runtime Flow

1. `MainApp` starts the JavaFX application.
2. `Bootstrap` creates the data directory, applies pending restore files, runs Flyway migrations, seeds default records, and validates the active outlet setting.
3. `AppShell` initializes locale/font support and hosts navigation.
4. JavaFX views call service classes for business operations.
5. Services use `Tx` to run Hibernate work against SQLite.
6. Reports and backups are generated from service/database state.

## Package Responsibilities

| Package | Responsibility |
| --- | --- |
| `db` | Application properties, Hibernate `SessionFactory`, SQLite connection setup |
| `entity` | JPA mapped domain objects such as outlets, members, entries, bills, rates, savings |
| `service` | Business workflows, transactions, billing, savings, lock checks, backups |
| `ui` | JavaFX screens, dialogs, navigation, table editing, operator actions |
| `report` | PDF bill and cap report generation |
| `i18n` | Locale initialization and resource lookup |

## Main Domain Model

```mermaid
erDiagram
    OUTLET ||--o{ MEMBER : has
    OUTLET ||--o{ RATE_PLAN : configures
    RATE_PLAN ||--o{ RATE_ITEM : contains
    MEMBER ||--o{ DAILY_MILK_ENTRY : supplies
    OUTLET ||--o{ DAILY_MILK_ENTRY : records
    MEMBER ||--o{ MONTHLY_BILL : receives
    MONTHLY_BILL ||--o{ BILL_ADJUSTMENT : includes
    SAVING_PERIOD ||--o{ MEMBER_SAVING : groups
    MEMBER ||--o{ MEMBER_SAVING : owns
```

## ERP Module View

MilkDiary is scoped like a small dairy ERP module: the outlet is the operating unit, members are suppliers, daily collection feeds billing, and billing feeds savings, reports, and audit/backup workflows.

```mermaid
flowchart TD
    Outlet["Outlet Master"] --> Members["Member / Supplier Master"]
    Outlet --> RatePlans["Rate Plan Management"]
    Members --> Daily["Daily Milk Collection"]
    RatePlans --> Daily
    Daily --> Billing["Monthly Billing"]
    Billing --> Adjustments["Bill Adjustments"]
    Billing --> Savings["Member Savings"]
    Billing --> Reports["PDF Bills / Cap Reports"]
    Daily --> Summary["Outlet Summary"]
    Billing --> Summary
    Settings["App Settings"] --> Reports
    Settings --> Backup["Backup / Restore"]
    Locks["Cap & Bill Locks"] --> Daily
    Locks --> Billing
    Audit["Audit Log"] --- Daily
    Audit --- Billing
    Audit --- Backup
```

## Billing Flow

```mermaid
sequenceDiagram
    participant UI as MonthlyBillingView
    participant Billing as BillingService
    participant Daily as DailyEntryService
    participant Rate as RateResolver
    participant DB as SQLite/Hibernate

    UI->>Billing: generate bill(outlet, member, month)
    Billing->>DB: load daily entries
    Billing->>Daily: compute rate and amount
    Daily->>Rate: resolve matching rate item
    Billing->>DB: upsert monthly bill
    Billing->>DB: update savings delta
    UI->>DB: reload generated bills
```

## Data And Migrations

The app uses SQLite for a low-friction desktop deployment. Flyway migrations live in `app/src/main/resources/db/migration` and create the schema incrementally.

Important operational data lives under `app/data` during local development. In a production packaging pass, the database path should be moved to an OS-specific application data directory.

## Cross-Cutting Concerns

- Transactions: service methods wrap database work through `Tx`.
- Localization: UI labels are loaded from resource bundles and Devanagari fonts are bundled.
- Auditability: sensitive operations such as backup, restore, cap locking, and daily entry changes are logged.
- Reporting: PDF generation uses OpenPDF and bundled fonts.
- Backup: SQLite backups are generated through application settings and retained by count.

## Known Technical Debt

- Add real tests under `app/src/test/java`.
- Harden SQLite foreign-key initialization and migration validation before production distribution.
- Move mutable runtime files out of the repository tree.
- Package the app with `jlink`/`jpackage` for non-developer installation.

## Recommended Refactor Path

The current architecture is acceptable for a portfolio desktop app: UI, service, entity, reporting, and database concerns are already separated enough to understand. I would not rewrite the whole app into a larger framework.

The useful next refactors are smaller and higher impact:

1. Introduce view models for large JavaFX screens such as `DailyEntriesView` and `MonthlyBillingView`.
2. Move billing aggregation into a pure calculation component that can be unit-tested without Hibernate.
3. Add repository/query classes only where service methods are becoming query-heavy.
4. Replace mutable runtime database files in the repository with seed scripts and a generated local DB.
5. Add integration tests using a temporary SQLite database and Flyway migrations.
6. Package the app with `jpackage` once the runtime data directory is moved outside the source tree.

Target shape:

```mermaid
flowchart LR
    View["JavaFX View"] --> VM["View Model / Screen State"]
    VM --> AppService["Application Service"]
    AppService --> Domain["Billing/Rate Domain Logic"]
    AppService --> Repo["Repository Queries"]
    Repo --> Hibernate["Hibernate + SQLite"]
    AppService --> Report["Report Export"]
```
