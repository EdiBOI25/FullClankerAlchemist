# Plan

A general guide on where to go next, not a list of every GitHub issue. Work is split into phases; issues are filed one phase at a time: finish a phase, then file the next one's issues. Tick items as they're done and change the plan only when the direction changes. Extra issues (fixes, chores) aren't listed here.

## Phase 1 – Data model
- [x] #3 feat: `Item` and `Tag` entities and repositories (item names unique ignoring case, tags lowercase, data kept across restarts)
- [x] #4 feat: `Recipe` entity and repository (pair stored ordered and unique, lookup by pair)
- [x] #5 feat: seed fire, water, earth and air at startup, without duplicating them on restart

## Phase 2 – Read API
- [ ] feat: `GET /items`
- [ ] feat: `GET /items?tag=` to filter by one or more tags
- [ ] feat: `GET /recipes`
- [ ] feat: standard error responses (`ProblemDetail`)

## Phase 3 – Combine
- [ ] chore: load `.env` and add `ANTHROPIC_API_KEY` to `.env.example`
- [ ] feat: `ItemGenerator` port and `ClaudeItemGenerator` (returns name, tags and emoji; fantasy and sci-fi items allowed)
- [ ] feat: `CombineService` returns a known recipe (A+B = B+A, an item can combine with itself)
- [ ] feat: reject items that aren't in the collection
- [ ] feat: an unknown pair generates a new item and saves the recipe
- [ ] feat: validate the AI result (name and at least one tag), retry once, then return an error
- [ ] feat: reuse an existing item when the names match, ignoring case
- [ ] feat: `POST /combine {a, b}`

## Phase 4 – Chat and MCP
- [ ] feat: `GameTools` (`list_items`, `combine`, `get_recipe`)
- [ ] feat: `POST /chat` using `GameTools`
- [ ] feat: MCP server that exposes `GameTools`
- [ ] docs: README section on using it from Postman and an MCP client

## Phase 5 – Icons
- [ ] feat: `IconProvider` port and `IconifyIconProvider`, saving `icon_url`
- [ ] feat: fall back to the emoji when Iconify fails or times out

## Phase 6 – RAG and vector store
- [ ] docs: research the embedding model and accept ADR-0006
- [ ] feat: `SimpleVectorStore` with local ONNX embeddings, saved to a JSON file
- [ ] feat: add each item to the vector store when it's created, base items included
- [ ] feat: rebuild the vector store from the DB if its file is missing
- [ ] feat: `GET /items/search?q=` (e.g. "something hot")
- [ ] feat: `search_items` tool for chat and MCP
- [ ] feat: show the AI similar existing items when combining

## Phase 7 – Docker
- [ ] chore: Dockerfile with the embedding model cached in the image
- [ ] chore: `compose.yaml` with the app and a volume for the DB and the vector file

## Not planned
- Drag-and-drop website (belongs in a frontend repo)
- Multiplayer, reset game, hints, Openverse photos
- CI
