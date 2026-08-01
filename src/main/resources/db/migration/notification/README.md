# `notification` migrations

Owns Postgres schema `notification`, including its own `notification.flyway_schema_history`. One Flyway instance per
module, so extracting this module in semester 2 is a datasource change — its whole
migration history travels with it.

## Versioning

**Numbering starts at `V1` and is local to this folder.** No coordination with other modules: their
history tables are in their own schemas, so `notification`'s `V1` and another module's `V1` never meet.

```
V1__<what_it_does>.sql
V2__<what_it_does>.sql
```

Never reuse a number. Never edit a migration that has already run — Flyway checksums applied
migrations, so fix forward with a new version instead.

## Rules

- Fully qualify objects with the schema: `CREATE TABLE notification.foo (...)`. Flyway sets `search_path`
  to `notification`, but being explicit survives copy-paste into psql.
- No foreign keys to another module's schema.
- No joins across schemas — call the other module's `api/` or subscribe to its event.
- Denormalise deliberately. Needing another module's column means copying it from an event payload.

The schema itself is created by Flyway (`createSchemas`), so the first migration here can go
straight to tables.
