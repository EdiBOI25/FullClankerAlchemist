# Architecture

Microservices, why: [ADR-0010](adr/0010-microservices.md). `alchemist-service` is hexagonal (ports & adapters), why: [ADR-0004](adr/0004-hexagonal-architecture.md).

> Target design: services and classes for later phases ([PLAN.md](PLAN.md)) are listed here before they exist.

## Services

| Service | Contains | Calls |
|---|---|---|
| `alchemist-service` | game, users, security, REST, chat, SQLite DB | `ai-service`, `chroma`, Iconify, AI provider (chat) |
| `ai-service` | item generation (`POST /generate`), internal only | AI provider |
| `mcp-server` | MCP tools for AI clients | `alchemist-service` REST, forwarding the `Authorization` header |
| `chroma` | vector store (stock image) | – |
| `zipkin` | trace viewer (stock image) | – |

```
MCP client ──► mcp-server ──► alchemist-service ──► ai-service ──► Gemini / Anthropic
Postman ─────────────────────►   │  │  │  │
                                 │  │  │  └──► Gemini / Anthropic (chat)
                                 │  │  └──► chroma
                                 │  └──► Iconify
                                 └──► SQLite (volume)
alchemist-service, ai-service, mcp-server ──► zipkin (traces)
```

Observability and resilience: [ADR-0010](adr/0010-microservices.md).

## Ports

| Port | Service | Published by compose |
|---|---|---|
| 2500 | `alchemist-service` | yes |
| 2501 | `ai-service` | no, internal (`alchemist-service` calls it on 2501) |
| 2502 | `mcp-server` | yes |
| 2503 | Zipkin UI | yes, mapped to its internal 9411 |
| 2504 | Chroma | only for debugging, mapped to its internal 8000 |
| 2505–2509 | spare | |

## Folder structure

```
compose.yaml                  all containers
pom.xml                       lists the modules
alchemist-service/            hexagonal
├── Dockerfile, pom.xml
└── src/main/java/fullclanker/alchemist/
    ├── domain/
    │   ├── model/            Item, Tag, Recipe, User (JPA entities)
    │   ├── dto/              ItemDto, RecipeDto, CombineRequest, CombineResult, GeneratedItem,
    │   │                     SignupRequest, ChatRequest (records)
    │   ├── exception/        ItemGenerationException, ItemNotFoundException,
    │   │                     AiServiceUnavailableException
    │   └── port/
    │       ├── in/           CombineItems, FindItems, FindRecipes, RegisterUser
    │       └── out/          ItemRepository, TagRepository, RecipeRepository, UserRepository,
    │                         ItemGenerator, ItemSearch, IconProvider
    ├── application/          CombineService, ItemService, RecipeService, UserService
    ├── adapter/
    │   ├── in/               ItemController, RecipeController, CombineController, AuthController,
    │   │                     ChatController, GameTools (chat)
    │   └── out/              HttpItemGenerator, ChromaItemSearch, IconifyIconProvider
    ├── config/               SecurityConfig, SqliteDialect, BaseItemSeeder, AdminSeeder,
    │                         AiProviderCheck, GeminiClientConfig, AiServiceClientConfig
    └── AlchemistApplication
ai-service/
├── Dockerfile, pom.xml
└── src/main/java/fullclanker/ai/
    ├── controller/           GenerateController, GenerateExceptionHandler
    ├── service/              ChatClientItemGenerator
    ├── dto/                  GenerateRequest, GeneratedItem
    ├── exception/            ItemGenerationException
    ├── config/               AiProviderCheck, GeminiClientConfig
    └── AiApplication
mcp-server/
└── src/main/java/fullclanker/mcp/
    ├── tool/                 AlchemistTools (list_items, combine, get_recipe, search_items)
    ├── dto/                  ItemDto, RecipeDto, CombineResult (copies of alchemist's JSON)
    ├── config/               tool registration, RestClient to alchemist-service
    └── McpApplication
```

`src/test/java` mirrors `src/main/java` in every module. Planned names; classes are added as the features get built.

## alchemist-service layers

| Layer | Package | Contains | May depend on |
|---|---|---|---|
| Domain | `domain` | entities (`model`), DTO records (`dto`), exceptions (`exception`), ports (`port.in`, `port.out`) | nothing |
| Application | `application` | services implementing `port.in` | `domain` |
| Adapters | `adapter.in`, `adapter.out` | REST, chat / ai-service, Chroma, Iconify | `application`, `domain` |
| Config | `config` | wiring, security | everything |

**Dependency rule:** `adapter → application → domain`. `domain` never imports `application` or `adapter`. `application` knows nothing about HTTP, chat or MCP.

- **in** = the outside calls the service (Postman, chat, `mcp-server`).
- **out** = the service calls the outside (DB, `ai-service`, Chroma, Iconify).

| Port | Direction | Implemented by |
|---|---|---|
| `CombineItems` | in | `CombineService` |
| `FindItems` | in | `ItemService` |
| `FindRecipes` | in | `RecipeService` |
| `RegisterUser` | in | `UserService` |
| `ItemRepository`, `TagRepository`, `RecipeRepository`, `UserRepository` | out | Spring Data (generated) |
| `ItemGenerator` | out | `HttpItemGenerator` (calls `ai-service`) |
| `ItemSearch` | out | `ChromaItemSearch` |
| `IconProvider` | out | `IconifyIconProvider` |

## Database

SQLite file owned by `alchemist-service`, see [ADR-0001](adr/0001-sqlite-database.md). Hibernate generates the schema from the entities, foreign keys enforced, see [ADR-0008](adr/0008-hibernate-generated-schema.md).

| Table | Columns |
|---|---|
| `item` | id, name (unique, case-insensitive), emoji, icon_url, discovered_by → user |
| `tag` | id, name (unique, lowercase) |
| `item_tag` | item_id → item, tag_id → tag |
| `recipe` | id, item_a_id → item, item_b_id → item, result_id → item, discovered_by → user |
| `user` | id, username (unique), password (bcrypt), role |
| `user_item` | user_id → user, item_id → item |

- Only the 4 base items are seeded (fire, water, earth, air); every new user gets them. Every other item is created when first discovered.
- Items, tags and recipes are shared; `user_item` is each user's collection ([ADR-0011](adr/0011-accounts-and-per-user-collections.md)).
- A recipe pair is stored ordered (`item_a_id ≤ item_b_id`) and is unique, so `A + B = B + A`. Combining an item with itself is allowed.
- Several recipes can lead to the same item (`fire + water` and `lava + water` → Steam).
- `scope=mine` recipes: both inputs and the result are in the user's collection.
- Every transaction starts with `BEGIN IMMEDIATE`, so concurrent writers queue (up to 3 s) instead of failing at once with `SQLITE_BUSY` ([ADR-0012](adr/0012-sqlite-immediate-transactions.md)).

**Vector store** (`chroma` container, details in the RAG phase ADR): one entry per item, text `"name: tag1, tag2"`, metadata `itemId`. Re-indexed from the DB if the collection is empty. `scope=mine` keeps only the user's items.

## Combine flow

A logged-in user combines `fire + water`:

```
CombineController (adapter.in)
  → CombineItems.combine(user, fire, water)          port.in
    → CombineService (application)
      1. both items in the user's collection? else error
      2. order the pair (a ≤ b)
      3. RecipeRepository: pair known? → go to 7
      4. ItemSearch: find similar existing items       port.out → chroma
      5. ItemGenerator: ask for the result             port.out → ai-service → AI provider
         (ai-service validates name, emoji and tags with Bean Validation and retries once on a failure or an invalid result;
          alchemist-service validates the reply again; unreachable → retry once; other failures → 502)
      6. in one transaction, after the AI call: pair saved meanwhile? → go to 7;
         reuse the item if the name exists, else save it (discovered_by, icon via IconProvider);
         save the recipe (discovered_by)
      7. add the result to the user's collection,
         return it with newForUser and firstDiscovery
```

Users arrive in the accounts phase (until then the collection is every item), step 4 in the RAG phase and icons in the last phase. Fallbacks: `ai-service` down → known pairs still work, new pairs return 503; Chroma down → step 4 is skipped; Iconify down → emoji. Why generate once: [ADR-0003](adr/0003-generate-combinations-once.md).

## Chat and MCP

- `POST /chat {message, tone}` → `ChatController` (adapter.in): `ChatClient` with `GameTools` (which call `port.in`), chat memory per user, tone or role in the system prompt.
- `mcp-server`: `AlchemistTools` call `alchemist-service`'s REST API with the MCP client's `Authorization` header, so they act as that user.

## Prompts

Prompt text lives in `src/main/resources/prompts/` of the service that calls the model (`ai-service`: combine, `alchemist-service`: chat), one StringTemplate file per role: `<feature>-system.st` (who the model is, the rules) and `<feature>-user.st` (the input, with `{placeholders}`).

- Don't describe the output format: `.entity(...)` appends the JSON schema.
- Escape literal `{` `}` (StringTemplate syntax).

## Decisions

All decisions are in [`adr/`](adr/).
