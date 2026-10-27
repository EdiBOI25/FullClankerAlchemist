# 8. Hibernate generates the schema

**Status:** Accepted (supersedes [ADR-0007](0007-hand-written-schema.md))

## Context
The ORM should be used for as much as possible. ADR-0007 kept a hand-written `schema.sql` because Hibernate's `SQLiteDialect` adds foreign keys with `ALTER TABLE`, which SQLite does not support, so it skips them. JPA 3.2's `options` attribute appends SQL to a generated column, so foreign keys can be declared inline. The stock dialect also silently drops multi-column unique constraints.

## Decision
- Hibernate generates the schema from the entities (`spring.jpa.hibernate.ddl-auto=update`). No `schema.sql`.
- Constraints are mapped with JPA 3.2:
  - foreign keys: `@JoinColumn(options = "REFERENCES item (id)")`, enforced with `?foreign_keys=true` on the JDBC URL;
  - checks: `@CheckConstraint`;
  - uniques: `@Column(unique = true)` and `@UniqueConstraint`; case-insensitive names with `options = "COLLATE NOCASE"`.
- `config.SqliteDialect` extends `SQLiteDialect` and writes multi-column unique constraints into `CREATE TABLE`.

## Consequences
- The entities are the only source of truth for the schema; the DB still rejects links to missing rows, not just JPA.
- `update` creates missing tables and columns but never changes existing ones. Changing a constraint on an existing table needs deleting `alchemist.db`.
- On startup Hibernate tries an `alter table ... drop constraint` that fails quietly; the constraint stays.
- A Hibernate upgrade may change the unique-constraint hook `SqliteDialect` relies on; the constraint tests catch it.
- If schema changes on existing tables become frequent, switch to a migration tool (Flyway).
