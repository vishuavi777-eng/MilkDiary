# Setup Guide

## Requirements

- JDK 23
- Git
- macOS, Windows, or Linux desktop environment capable of running JavaFX

## Clone

```bash
git clone git@github.com:vishuavi777-eng/MilkDiary.git
cd MilkDiary
```

## Run

```bash
./gradlew :app:run
```

Windows:

```bat
gradlew.bat :app:run
```

## Test

```bash
./gradlew :app:test
```

## Database

During development the app uses:

```text
app/data/milkdiary.db
```

The database schema is managed by Flyway migrations in:

```text
app/src/main/resources/db/migration
```

## Useful Gradle Commands

```bash
./gradlew :app:compileJava
./gradlew :app:test
./gradlew :app:run
./gradlew clean
```

## Troubleshooting

If startup fails after schema changes, inspect the local SQLite database and Flyway history before deleting data. For portfolio/demo work, keep a backup copy of `app/data/milkdiary.db` before migration experiments.
