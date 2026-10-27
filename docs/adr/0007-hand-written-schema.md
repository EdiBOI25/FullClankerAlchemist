# 7. Hand-written schema with enforced foreign keys

**Status:** Superseded by [ADR-0008](0008-hibernate-generated-schema.md)

## Context
Hibernate adds foreign keys with `ALTER TABLE`, which SQLite does not support, so a Hibernate-generated schema has none. SQLite also enforces foreign keys only on connections that turn them on.

## Decision
- The schema lives in `src/main/resources/schema.sql` (`CREATE TABLE IF NOT EXISTS`) and runs on every startup (`spring.sql.init.mode=always`). Hibernate does not generate or change it.
- Foreign keys are declared in `schema.sql` and enforced with `?foreign_keys=true` on the JDBC URL.

## Consequences
- The DB rejects links to missing rows, not just JPA.
- `schema.sql` is the source of truth; JPA annotations only map to it.
- A new table is one more `CREATE TABLE IF NOT EXISTS`. Changing an existing table needs a manual `ALTER` or deleting `alchemist.db`.
- If schema changes on existing tables become frequent, switch to a migration tool (Flyway).
