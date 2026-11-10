# FullClankerAlchemist

Little Alchemy, but infinite. Start with the four base elements, combine any two, and the AI invents the result.

## How it plays
1. Start with **fire**, **water**, **earth** and **air**.
2. Pick two items and combine them: `ice + fire = water`.
3. Every new item gets a name, tags (e.g. `nature`, `liquid`, `creature`) and an icon.
4. Discovered items join your collection and can be combined further.

A combination is generated once and then remembered, so the same pair always gives the same result.

## Run it
Needs Docker and a Gemini or Anthropic API key. On Linux, first run `mkdir -p data && sudo chown 1001 data` (the app runs as user 1001).

```bash
cp .env.example .env    # set AI_PROVIDER and its API key
docker compose up -d --build
```

Open http://localhost:2500/items for the four base elements, then combine two:

```bash
curl -X POST localhost:2500/combine -H "Content-Type: application/json" -d '{"a":"fire","b":"water"}'
```

- Logs and traces: [docs/LOGS.md](docs/LOGS.md), Zipkin at http://localhost:2503
- On a server: [docs/SELF-HOSTING.md](docs/SELF-HOSTING.md)
- Developing (build, tests, running from the IDE): [AGENTS.md](AGENTS.md) and [CONTRIBUTING.md](CONTRIBUTING.md)

---
*This is a collaborative college project.*
