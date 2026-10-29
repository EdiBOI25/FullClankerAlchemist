# Architecture

Hexagonal architecture (ports & adapters). Why: [ADR-0004](adr/0004-hexagonal-architecture.md).

## Layers

| Layer | Package | Contains | May depend on |
|---|---|---|---|
| Domain | `domain` | entities (`model`), DTO records (`dto`), ports (`port.in`, `port.out`) | nothing |
| Application | `application` | services implementing `port.in` | `domain` |
| Adapters | `adapter.in`, `adapter.out` | REST, chat, MCP / AI provider, Iconify, vector store | `application`, `domain` |
| Config | `config` | wiring | everything |

**Dependency rule:** `adapter → application → domain`. `domain` never imports `application` or `adapter`. `application` knows nothing about HTTP, chat or MCP.

- **in** = the outside calls the app (Postman, chat, MCP client).
- **out** = the app calls the outside (DB, AI provider, Iconify, vector store).

## Folder structure

```
lab.site.backend
├── domain/
│   ├── model/        Item, Tag, Recipe (JPA entities)
│   ├── dto/          ItemDto, RecipeDto, CombineRequest, CombineResult (records)
│   └── port/
│       ├── in/       CombineItems, FindItems, FindRecipes
│       └── out/      ItemRepository, TagRepository, RecipeRepository, ItemGenerator, IconProvider, ItemSearch
├── application/      CombineService, ItemService, RecipeService
├── adapter/
│   ├── in/           ItemController, RecipeController, CombineController, ChatController, GameTools (chat + MCP)
│   └── out/          ChatClientItemGenerator, IconifyIconProvider, VectorItemSearch
├── config/           wiring, SqliteDialect, BaseItemSeeder, AiProviderCheck
└── Application.java
```

Planned names; classes are added as the features get built.

## Ports

| Port | Direction | Implemented by |
|---|---|---|
| `CombineItems` | in | `CombineService` |
| `FindItems` | in | `ItemService` |
| `FindRecipes` | in | `RecipeService` |
| `ItemRepository`, `TagRepository`, `RecipeRepository` | out | Spring Data (generated) |
| `ItemGenerator` | out | `ChatClientItemGenerator` |
| `IconProvider` | out | `IconifyIconProvider` |
| `ItemSearch` | out | `VectorItemSearch` |

## Database

SQLite file, see [ADR-0001](adr/0001-sqlite-database.md). Hibernate generates the schema from the entities, foreign keys enforced, see [ADR-0008](adr/0008-hibernate-generated-schema.md).

| Table | Columns |
|---|---|
| `item` | id, name (unique, case-insensitive), emoji, icon_url |
| `tag` | id, name (unique, lowercase) |
| `item_tag` | item_id → item, tag_id → tag |
| `recipe` | id, item_a_id → item, item_b_id → item, result_id → item |

- Only the 4 base items are seeded (fire, water, earth, air). Every other item is created when first discovered ([ADR-0002](adr/0002-single-player.md)).
- A recipe pair is stored ordered (`item_a_id ≤ item_b_id`) and is unique, so `A + B = B + A`. Combining an item with itself is allowed.
- Several recipes can lead to the same item (`fire + water` and `lava + water` → Steam).

**Vector store** (separate JSON file, [ADR-0006](adr/0006-vectorstore-local-embeddings.md)): one entry per item, text `"name: tag1, tag2"`, metadata `itemId`. Rebuilt from the DB if the file is missing.

## Combine flow

`fire + water`:

```
CombineController (adapter.in)
  → CombineItems.combine(fire, water)              port.in
    → CombineService (application)
      1. order the pair (a ≤ b)
      2. RecipeRepository: pair known? → return its result
      3. ItemSearch: find similar existing items   port.out
      4. ItemGenerator: ask the AI for the result  port.out
      5. validate (name + at least one tag), retry once
      6. reuse the item if the name exists, else save it (icon via IconProvider)
      7. save the recipe, return the result
```

Steps 3 and 6 (icon) arrive in later phases; until then they are skipped. Why generate once: [ADR-0003](adr/0003-generate-combinations-once.md).

## Prompts

Prompt text lives in `src/main/resources/prompts/`, one StringTemplate file per role: `<feature>-system.st` (who the model is, the rules) and `<feature>-user.st` (the input, with `{placeholders}`).

- Don't describe the output format: `.entity(...)` appends the JSON schema.
- Escape literal `{` `}` (StringTemplate syntax).

## Decisions

All decisions are in [`adr/`](adr/).
