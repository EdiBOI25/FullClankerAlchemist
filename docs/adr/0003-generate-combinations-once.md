# 3. Generate each combination once

**Status:** Accepted

## Context
AI results are not deterministic, and every AI call costs time and money.

## Decision
A new pair calls the AI once; the result is saved as a recipe. Later calls for the same pair read it from the DB. The pair is stored ordered (`a ≤ b`), so `A + B = B + A`. Combining an item with itself (`water + water`) is allowed.

## Consequences
- The same pair always gives the same result.
- The AI is called only for pairs nobody has tried yet.
- A bad AI result stays until it is deleted manually.
