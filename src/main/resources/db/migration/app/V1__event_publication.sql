-- Spring Modulith's event publication registry.
--
-- This is the table that makes in-process events durable: an event is written here inside the
-- publishing transaction and marked complete once each listener has run. It is, in effect, the
-- outbox that Phase 6 of the plan calls for -- which is why switching to Kafka in semester 2 only
-- swaps the publisher, not this table.
--
-- It belongs to no business module: every module publishes into it. Hence `db/migration/app`,
-- running against `goezyticket.migration.app-schema` (default `public`) rather than a schema of
-- its own. Objects are intentionally left unqualified so they follow Flyway's default schema.
--
-- The column definitions are Hibernate's own, generated from Modulith 2.0.7's JpaEventPublication
-- mapping via `jakarta.persistence.schema-generation`. They must keep matching it, because
-- `ddl-auto: validate` checks this table on every startup. Regenerate after a Modulith upgrade
-- rather than hand-editing.

CREATE TABLE event_publication
(
    id                     uuid                     NOT NULL,
    listener_id            varchar(255)             NOT NULL,
    event_type             varchar(255)             NOT NULL,
    -- Hibernate's default mapping is varchar(255), widened deliberately: this holds the serialized
    -- event, and any payload with a couple of UUIDs and an email exceeds 255 characters. Postgres
    -- reports `text` as JDBC VARCHAR, so schema validation still passes.
    serialized_event       text                     NOT NULL,
    status                 varchar(255) CHECK (status IN
                                               ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED')),
    completion_attempts    integer                  NOT NULL,
    publication_date       timestamp(6) with time zone NOT NULL,
    completion_date        timestamp(6) with time zone,
    last_resubmission_date timestamp(6) with time zone,
    PRIMARY KEY (id)
);

COMMENT ON TABLE event_publication IS
    'Spring Modulith event registry / outbox. Owned by the application, not by any module.';

-- Deliberately created with no secondary indexes, against the instinct to add them.
--
-- Guiding principle 1 of the plan: never optimize what you have not measured. The three queries
-- Modulith actually runs against this table are below, so the candidate indexes are known and can
-- be added with a before/after number attached -- a ready-made Phase 4 experiment rather than a
-- guess baked into the schema.
--
--   1. Completion, on every handled event (hot path):
--        WHERE serialized_event = ? AND listener_id = ? AND completion_date IS NULL
--      Candidate: a HASH index on serialized_event. Not btree -- the column is unbounded text and
--      only ever compared with equality.
--
--   2. Republishing incomplete events on startup:
--        WHERE completion_date IS NULL ORDER BY publication_date ASC
--      Candidate: a partial btree on (publication_date) WHERE completion_date IS NULL.
--
--   3. Purging completed events:
--        WHERE completion_date IS NOT NULL / completion_date < ?
--      Candidate: btree on (completion_date).
--
-- Expect query 1 to be the one that shows up first: it runs once per listener per event, so a
-- sequential scan over a growing table degrades exactly when the flash sale is busiest.
