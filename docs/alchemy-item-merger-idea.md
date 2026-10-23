# Alchemy Item Merger

Little Alchemy, but infinite. Start with the base elements; combine any two and the AI invents the result.

## Core loop
1. Start with base items: fire, water, earth, air.
2. Pick two items -> **combine** (`ice + fire = water`).
3. Known pair -> result from DB. New pair -> AI generates the item (name, tags, icon), saved for next time.
4. New item added to discovered items.

## Requirements mapping
| Requirement | How |
|---|---|
| DB | SQLite via JPA/Hibernate: items (name, tags, icon), recipes (pair -> result, order-independent), discovered items |
| REST | `GET /items`, `POST /combine {a, b}`, `GET /recipes` |
| AI | Combination generation via Spring AI (Anthropic), structured output + tool calling (`find_item` to reuse existing items) |
| MCP | Same tools exposed to external clients: `list_items`, `combine`, `get_recipe` |
| Docker | App image + mounted volume for `alchemist.db` |

## Icons
- Iconify search API (keyless): `https://api.iconify.design/search?query=dinosaur`
- Fallback: emoji returned by the AI.

## Later / maybe
- Openverse photos (keyless, CC licensed): `https://api.openverse.org/v1/images/?q=dinosaur`
- Hints for undiscovered combinations.
- Filter/browse items by tag.
