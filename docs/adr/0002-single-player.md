# 2. Single player, no accounts

**Status:** Accepted

## Context
College project. Accounts (login, logout, auth) are not required.

## Decision
One player, no accounts. The player's collection is every item in the DB. Only the 4 base items are seeded (fire, water, earth, air); every other item is created the first time the AI discovers it.

## Consequences
- No user or auth tables.
- Multiplayer or a "reset game" feature would need a per-player discovered-items table.
