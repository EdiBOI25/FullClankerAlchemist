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
cp .env.example .env    # set AI_PROVIDER and its API key (ADMIN_USERNAME and ADMIN_PASSWORD are optional)
docker compose up -d --build
```

Every endpoint except sign-up, login, logout and `/actuator/health` needs a login. Sign up, log in (curl keeps the session cookie in `cookies.txt`), list the four base elements, combine two, and log out (on Windows, use Git Bash or WSL):

```bash
curl -X POST localhost:2500/auth/signup -H "Content-Type: application/json" -d '{"username":"alice","password":"choose-a-password"}'
curl -c cookies.txt -X POST localhost:2500/auth/login -H "Content-Type: application/json" -d '{"username":"alice","password":"choose-a-password"}'
curl -b cookies.txt localhost:2500/items
curl -b cookies.txt -X POST localhost:2500/combine -H "Content-Type: application/json" -d '{"a":"fire","b":"water"}'
curl -b cookies.txt -X POST localhost:2500/auth/logout
```

Usernames are 3–30 characters (`a-z`, `A-Z`, `0-9`, `_`, `-`), passwords at least 8. Scripts can skip the session and use HTTP Basic instead: `curl -u alice localhost:2500/items` (curl asks for the password).

- Logs and traces: [docs/LOGS.md](docs/LOGS.md), Zipkin at http://localhost:2503
- On a server: [docs/SELF-HOSTING.md](docs/SELF-HOSTING.md)
- Developing (build, tests, running from the IDE): [AGENTS.md](AGENTS.md) and [CONTRIBUTING.md](CONTRIBUTING.md)

---
*This is a collaborative college project.*
