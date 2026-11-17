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
cp .env.example .env    # set AI_PROVIDER, its API key, ADMIN_USERNAME and ADMIN_PASSWORD
docker compose up -d --build
```

Every endpoint except `/actuator/health` needs a login (HTTP Basic). Until sign-up exists, use the admin from `.env`: replace `admin` with your `ADMIN_USERNAME`, and curl asks for the password. List the four base elements, then combine two (on Windows, use Git Bash or WSL):

```bash
curl -u admin localhost:2500/items
curl -u admin -X POST localhost:2500/combine -H "Content-Type: application/json" -d '{"a":"fire","b":"water"}'
```

- Logs and traces: [docs/LOGS.md](docs/LOGS.md), Zipkin at http://localhost:2503
- On a server: [docs/SELF-HOSTING.md](docs/SELF-HOSTING.md)
- Developing (build, tests, running from the IDE): [AGENTS.md](AGENTS.md) and [CONTRIBUTING.md](CONTRIBUTING.md)

---
*This is a collaborative college project.*
