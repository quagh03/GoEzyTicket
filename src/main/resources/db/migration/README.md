# Migrations

One folder per business module, one Flyway instance per folder, plus one `app` folder for tables
that belong to no module. `platform` and `shared` hold no state and have no folder.

| Folder | Schema | History table |
|---|---|---|
| **app** | `public` (`goezyticket.migration.app-schema`) | `public.flyway_schema_history` |
| identity | `identity` | `identity.flyway_schema_history` |
| catalog | `catalog` | `catalog.flyway_schema_history` |
| inventory | `inventory` | `inventory.flyway_schema_history` |
| ordering | `ordering` | `ordering.flyway_schema_history` |
| payment | `payment` | `payment.flyway_schema_history` |
| ticketing | `ticketing` | `ticketing.flyway_schema_history` |
| notification | `notification` | `notification.flyway_schema_history` |

**Every folder numbers its migrations from `V1`.** There is no shared sequence and no cross-module
coordination, because no two folders share a history table.

## The `app` folder

Infrastructure owned by the application rather than by a module. Today that is exactly one table:
Spring Modulith's `event_publication` registry — the outbox every module publishes into. It runs
first, before the modules, and targets the database's default schema.

It exists because `ddl-auto: validate` means Hibernate no longer creates that table for you. Its
columns must keep matching Modulith's `JpaEventPublication` mapping; after a Modulith upgrade,
regenerate rather than hand-edit:

```
./gradlew bootRun --args="--spring.jpa.hibernate.ddl-auto=none \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=/tmp/schema.sql"
```

Resist putting business tables here. "Two modules need it" means the boundary is cut wrong.

## How it is wired

`goezyticket.migration.modules` in `application.yaml` lists the modules. `ModularFlywayMigrationStrategy`
(in `com.huylq.goezyticket.config`) builds one Flyway instance per entry, each with
`schemas = defaultSchema = <module>`, and runs them in order.

It is installed as a Spring Boot `FlywayMigrationStrategy` rather than as a set of custom beans. Boot
still creates its own `Flyway` bean and its `FlywayMigrationInitializer`; the strategy replaces what
that initializer *does* without changing *when* it runs — which is what keeps the guarantee that
migrations complete before JPA starts. The autoconfigured instance is never migrated; only its
`DataSource` is used.

## Who creates the schemas

Flyway, via `createSchemas`, on every startup — nothing else. Point the app at an empty database and
it creates all seven schemas plus their history tables before JPA starts.

There is deliberately no `docker-entrypoint-initdb.d` script. A second copy of the module list would
be one more place to forget when adding a module, and it would only ever run on a fresh volume
anyway. Local dev, CI and Testcontainers now all get their schemas from the same single list in
`goezyticket.migration.modules`.

One consequence worth knowing: the database user needs `CREATE` on the database. Fine for the
`postgres` superuser used in dev; if a restricted user is introduced later, grant it explicitly or
schema creation fails at startup.

## Adding a module

1. Add its name to `goezyticket.migration.modules`.
2. Create `db/migration/<module>/`.
3. Write `V1__....sql`.
