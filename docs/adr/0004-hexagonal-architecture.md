# 4. Hexagonal architecture

**Status:** Accepted

## Context
Two developers and several AI agents (Claude Code, Copilot, Junie, Codex) work on the code. We want clear contracts and logic that does not depend on the web layer. The starting idea was three layers: `include` (contracts), `lib` (reusable logic), `src` (wiring).

## Decision
Hexagonal architecture (ports & adapters), with the usual Java names:

| Original idea | Package |
|---|---|
| `include` | `domain` (entities, DTOs, ports) |
| `lib` | `application`, `adapter.out` |
| `src` | `adapter.in`, `config` |

- Dependency rule: `adapter → application → domain`.
- Interfaces only at the boundaries (ports), not for every class.
- JPA entities live in `domain`, annotations included (pragmatic choice).
- Not enforced by tooling; the rule is written in `AGENTS.md`.

Details: [ARCHITECTURE.md](../ARCHITECTURE.md).

## Consequences
- Developers and agents know where new code goes; the names are widely known.
- AI, icons and search sit behind ports, so they can be swapped or faked in tests.
- More packages than a tiny app strictly needs.
- The dependency rule relies on discipline.
