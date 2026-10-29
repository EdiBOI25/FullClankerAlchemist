# AGENTS.md

Instructions for AI coding agents (Claude Code, Copilot, Junie, Codex) and humans.

## Project
Alchemy Item Merger: combine two items, the AI invents the result. See [README.md](README.md) and [docs/FEATURES.md](docs/FEATURES.md).

Stack: Java 21, Spring Boot 4.1, Spring AI 2.0 (Gemini or Anthropic), SQLite via JPA/Hibernate, Maven. Root package: `lab.site.backend`.

## Commands
- Build and test: `./mvnw verify` (Windows: `mvnw.cmd verify`)
- Run: `./mvnw spring-boot:run`
- Secrets: copy `.env.example` to `.env` and fill it in.

## Architecture
Hexagonal (ports & adapters). Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before writing code.

- `domain`: entities, DTO records, ports (interfaces). Imports nothing from `application` or `adapter`.
- `application`: services implementing `port.in`. No HTTP, chat or MCP types.
- `adapter.in`: REST, chat, MCP. Calls `port.in` only.
- `adapter.out`: implements `port.out` (AI provider, Iconify, vector store).
- `config`: wiring.

**Dependency rule:** `adapter → application → domain`. Nothing enforces it automatically, so check every import you add.

- Interfaces only for ports; no interface for a single internal class.
- Decisions live in [docs/adr/](docs/adr/). Don't contradict an accepted ADR; propose a new one instead.

## Workflow
- One issue = one branch = one PR into `master`. Direct pushes to `master` are blocked.
- Branch names: `type/short-description` with `feature`, `fix`, `docs`, `chore`, `refactor`, `test`.
- Commits: `type: description` (Conventional Commits, `feat` instead of `feature`).
- Keep changes small: one slice per PR.
- Every `feat`/`fix` issue names the behaviour its tests must cover; the PR adds those tests. Never edit a failing test just to make it pass.
- Never commit `.env`, `*.db` or the vector store file.
- Propose a plan before larger changes. Don't push or open PRs unless asked.

Details: [CONTRIBUTING.md](CONTRIBUTING.md).
