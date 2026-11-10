# 12. SQLite transactions start with BEGIN IMMEDIATE

**Status:** Accepted

## Context
Two concurrent first combines of the same pair, or of two pairs that produce the same new item, both find nothing saved and then both write. A deferred SQLite transaction starts as a reader and becomes a writer on its first write. When two readers try that at once, SQLite fails one immediately with `SQLITE_BUSY` (the busy timeout doesn't apply), so the request returned a 500. The unique-violation handling added for #45 never ran, because the race never reached a constraint.

## Decision
- sqlite-jdbc starts every transaction with `BEGIN IMMEDIATE` (`spring.datasource.hikari.data-source-properties.transaction_mode=IMMEDIATE`). It takes the write lock up front, so a second writer waits (sqlite-jdbc's busy timeout, 3 s) and then sees the first one's rows.
- `CombineService` calls the AI outside the transaction, then re-checks the pair and the item name inside it.
- `spring.jpa.open-in-view=false`, so a request holds no DB connection during the AI call.
- `CombineServiceConcurrencyTest` runs both races against a file DB.

## Consequences
- No `SQLITE_BUSY` on concurrent combines, and no retry code.
- Every transaction, read-only ones included (Spring Data's `findAll`, `findById`), takes the write lock, so they queue behind writes. Reads outside a transaction (derived queries) don't wait. Fine at this app's load.
- A lock wait over 3 s returns a 500.
- sqlite-jdbc's `commit()` starts the next transaction right away (`commit; begin immediate;`), so each transaction takes the write lock twice. Under heavy contention that second `BEGIN` can time out after the commit succeeded: the caller gets a 500, the data is saved, and a retry finds it.
- Not chosen for now:
  - WAL: measured, no gain while every transaction is `IMMEDIATE`; it adds `-wal`/`-shm` files to back up.
  - `jdbc.explicit_readonly`: would keep read-only transactions deferred, but breaks with a connection pool (sqlite-jdbc rejects the write upgrade once the connection has run any statement).
  - Retrying on `SQLITE_BUSY`: more code, and it can fail again under load.
- If reads start queueing: a second, read-only pool (deferred transactions, WAL) for `@Transactional(readOnly = true)`. If writes outgrow one writer: PostgreSQL, superseding ADR-0001.
