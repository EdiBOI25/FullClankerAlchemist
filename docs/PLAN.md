# Plan

A general guide on where to go next, not a list of every GitHub issue. Work is split into phases; issues are filed one phase at a time: finish a phase, then file the next one's issues. Tick items as they're done and change the plan only when the direction changes. Extra issues (fixes, chores) aren't listed here.

Every item validates all its inputs and tests every behaviour, failure paths included. Teacher's requirements: [FEATURES.md](FEATURES.md#project-requirements).

## Phase 1 – Data model
- [x] #3 feat: `Item` and `Tag` entities and repositories (item names unique ignoring case, tags lowercase, data kept across restarts)
- [x] #4 feat: `Recipe` entity and repository (pair stored ordered and unique, lookup by pair)
- [x] #5 feat: seed fire, water, earth and air at startup, without duplicating them on restart

## Phase 2 – Read API
- [x] #13 feat: `GET /items`
- [x] #14 feat: `GET /items?tag=` to filter by one or more tags
- [x] #15 feat: `GET /recipes`

## Phase 3 – Combine
Validation with Bean Validation; tests mock `ItemGenerator` with Mockito.
- [x] #19 chore: load `.env` and configure Gemini (default) and Anthropic, picked by `AI_PROVIDER`
- [x] #20 feat: `ItemGenerator` port and Spring AI generator (returns name, tags and emoji; fantasy and sci-fi items allowed)
- [x] #21 feat: `CombineService` returns a known recipe (A+B = B+A, an item can combine with itself)
- [x] #22 feat: reject items that aren't in the collection
- [x] #23 feat: an unknown pair generates a new item and saves the recipe
- [x] #24 feat: validate the AI result (name and at least one tag), retry once, then return an error
- [x] #25 feat: reuse an existing item when the names match, ignoring case
- [x] #26 feat: `POST /combine {a, b}` (errors as `ProblemDetail`)

## Phase 4 – Microservices, Docker, observability, resilience
- [x] #39 refactor: move the app into `alchemist-service/` with a root `pom.xml` (pure `git mv` first), then rename it to `fullclanker.alchemist`
- [x] #40 feat: `ai-service` with `POST /generate` (generator, prompts, validation and retry copied from `alchemist-service`; #41 removes the originals)
- [x] #41 feat: `HttpItemGenerator` calls `ai-service`: timeout longer than `ai-service`'s worst case, retry only when it can't be reached; known pairs still combine when it's down, new pairs return 503
- [ ] #45 fix: AI call outside the DB transaction; concurrent first tries of a pair or a name return the saved result
- [ ] #42 chore: a Dockerfile per service and `compose.yaml` (DB on a volume, `ai-service` internal only)
- [ ] #43 feat: Actuator health and metrics in every service, used by compose healthchecks
- [ ] #44 feat: tracing and app logs across services (trace id in the logs), Zipkin in compose
- [ ] #46 docs: AGENTS.md, README and ARCHITECTURE for the new layout and `docker compose up`

## Phase 5 – Accounts and per-user collections
- [ ] feat: `User` entity with roles (`USER`, `ADMIN`), admin account from `.env`
- [ ] feat: sign up (gets the 4 base items), login and logout; decide CSRF
- [ ] feat: `user_item`: each user combines only their own items and sees them by default
- [ ] feat: `scope=mine|all` for items and recipes (e.g. `GET /recipes?scope=all&item=iron`)
- [ ] feat: `discovered_by` on items and recipes
- [ ] feat: combine returns `newForUser` and `firstDiscovery`
- [ ] feat: admin-only `DELETE /recipes/{id}` (recipes expose their id)

## Phase 6 – Chat
- [ ] feat: `GameTools` (`list_items` and `get_recipe` with `scope`, plus `combine`)
- [ ] feat: `POST /chat {message, tone}` using `GameTools`
- [ ] feat: chat memory per user
- [ ] feat: tone or role in the system prompt
- [ ] test: AI-judged tests (Spring AI evaluator) for the generation and the chat, live only

## Phase 7 – RAG with Chroma
- [ ] docs: research the embedding model, ADR for Chroma
- [ ] chore: `chroma` in compose
- [ ] feat: add each item to Chroma when it's created, base items included
- [ ] feat: re-index from the DB if the Chroma collection is empty
- [ ] feat: `GET /items/search?q=&scope=` (e.g. "something hot")
- [ ] feat: `search_items` tool for chat
- [ ] feat: send similar existing items to `ai-service` when combining; combine still works when Chroma is down

## Phase 8 – MCP server
- [ ] chore: spike: can an MCP tool read the request's `Authorization` header?
- [ ] feat: `mcp-server` with `list_items`, `combine`, `get_recipe` and `search_items`, forwarding the `Authorization` header
- [ ] chore: `mcp-server` in compose
- [ ] docs: README section on using it from Postman and an MCP client

## Phase 9 – Icons (optional)
- [ ] feat: `IconProvider` port and `IconifyIconProvider`, saving `icon_url`
- [ ] feat: fall back to the emoji when Iconify fails or times out

## Not planned
- Drag-and-drop website (belongs in a frontend repo)
- Reset game, hints, Openverse photos
- CI
- `user_recipe` table, personal access tokens or OAuth for MCP clients, gateway, circuit breaker
