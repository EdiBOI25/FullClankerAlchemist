# 10. Microservices

**Status:** Accepted (supersedes [ADR-0005](0005-shared-tools-chat-mcp.md))

## Context
The teacher requires a microservices-based architecture (mark 5), Docker with a `compose.yaml` running several containers (mark 6) and an MCP server (mark 10), and asked for observability and resilience. Until now the project is one Spring Boot app.

## Decision
Five containers, started by one `compose.yaml` (project name `fullclanker-alchemist`):

| Service | Role | Reachable from outside |
|---|---|---|
| `alchemist-service` | the game: items, recipes, users, security, combine, chat, search; owns the SQLite DB | yes |
| `ai-service` | generates a new item for a pair (`POST /generate`) | no |
| `mcp-server` | MCP tools for AI clients; calls `alchemist-service`'s REST API | yes |
| `chroma` | vector store (stock image) | no |
| `zipkin` | trace viewer (stock image) | yes |

- Calls go one way: `mcp-server → alchemist-service → ai-service`. `ai-service` never calls back.
- Chat stays in `alchemist-service`. Its tools change the user's collection, so chat in `ai-service` would make the two services call each other and pass the user's login along.
- `alchemist-service` stays hexagonal ([ADR-0004](0004-hexagonal-architecture.md)); its `ItemGenerator` port gets an HTTP adapter that calls `ai-service`. `ai-service` and `mcp-server` are small and use plain folders (`controller`, `service`, `tool`, `dto`, `config`).
- `mcp-server` has its own `@Tool` methods that call the REST API; chat keeps `GameTools`, which call the inbound ports. This replaces [ADR-0005](0005-shared-tools-chat-mcp.md).
- Services share no code; `mcp-server` keeps its own copies of the JSON records it reads.
- One Maven module per service, in a folder named like its container; the root `pom.xml` only lists the modules. Packages: `fullclanker.alchemist`, `fullclanker.ai`, `fullclanker.mcp`.
- Observability: Actuator health and metrics in every service (compose healthchecks use them); Micrometer Tracing passes the trace id between services and sends the spans to Zipkin; the trace id appears in every log line.
- Resilience: a timeout on every outbound call. `ai-service` validates the AI result and retries once; `alchemist-service` retries only when it can't reach `ai-service`, with a timeout longer than `ai-service`'s worst case, so retries don't stack. Fallbacks: known pairs combine when `ai-service` is down, combine works without similar items when Chroma is down, emoji when Iconify is down.

## Consequences
- Each service builds, runs and fails on its own; `docker compose up` starts everything.
- The AI provider config (`AiProviderCheck`, `GeminiClientConfig`, keys) exists twice: in `alchemist-service` for chat and in `ai-service` for generation.
- `ai-service` has no authentication; only the Docker network reaches it.
- A new pair costs one more HTTP call; when `ai-service` is down, new pairs return 503.
- No gateway and no circuit breaker; add them if the teacher asks.
- Chroma's details (embedding model, indexing) get their own ADR in the RAG phase.
