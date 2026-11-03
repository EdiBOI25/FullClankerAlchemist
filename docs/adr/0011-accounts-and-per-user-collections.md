# 11. Accounts and per-user collections

**Status:** Accepted (supersedes [ADR-0002](0002-single-player.md))

## Context
The teacher requires Spring Security with authentication and authorization (mark 9). [ADR-0002](0002-single-player.md) had one player and no accounts. With several players each needs their own collection, but generating the same pair again for every player would waste AI calls ([ADR-0003](0003-generate-combinations-once.md)).

## Decision
- Users sign up, log in and log out. Login keeps a session (cookie); HTTP Basic is also accepted (for `mcp-server` and scripts). Only sign up, login and `/actuator/health` are public.
- `user` table (JPA entity): username (unique), password (bcrypt), role (`USER` or `ADMIN`). The admin account comes from `.env`.
- Every new user, the admin included, gets the 4 base items.
- Items, tags and recipes are shared by all users. A user's collection is the `user_item` join table (user_id, item_id). A pair is generated once for everyone.
- A user can only combine items from their own collection.
- Item and recipe lists and search take `scope=mine|all`, `mine` by default. "My recipes" are the recipes whose two inputs and result are all in the user's collection; there is no `user_recipe` table.
- `item.discovered_by` is the user who first created the item; `recipe.discovered_by` is the user who first tried the pair. Both are null for the base items.
- A combine returns `newForUser` (the user didn't have the item) and `firstDiscovery` (the item didn't exist before).
- Only `ADMIN` can delete a recipe (`DELETE /recipes/{id}`), to remove bad AI results. Only the recipe goes; its result item stays in every collection.
- `mcp-server` forwards the MCP client's `Authorization` header, so its tools act as that user.

## Consequences
- The AI is still called once per pair, whoever tries it first, with two exceptions:
  - two users trying a new pair at the same moment both call it; the second save hits the unique pair and returns the recipe the first one saved;
  - after a recipe is deleted, the next try of that pair calls it again.
- An item whose recipes were all deleted stays, with no known way to make it.
- "My recipes" can include a pair the user never tried, if another user found it and the user owns all three items. Add `user_recipe` if an exact history is needed.
- Chat memory and search are per user.
- MCP clients keep the user's password in their config; personal access tokens would fix that (not planned).
- CSRF protection is decided when security is built.
- SQLite still allows one writer at a time ([ADR-0001](0001-sqlite-database.md)); fine for a few players.
