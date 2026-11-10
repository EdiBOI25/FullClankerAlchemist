# Self-hosting

Both services on a Linux server with Docker Compose, behind Nginx Proxy Manager, reached over WireGuard only.

## First setup
1. Install Docker Engine with the compose and buildx plugins (Docker's official install has both).
2. Clone and configure:
   ```bash
   git clone https://github.com/EdiBOI25/FullClankerAlchemist.git
   cd FullClankerAlchemist
   cp .env.example .env
   ```
   In `.env`: set `AI_PROVIDER` and its API key. Optional: `ALCHEMIST_PORT` (default `2500`), `ALCHEMIST_DATA_DIR` (default `./data`; must be a path starting with `./` or `/`).
3. Create the data folder; the app runs as user 1001 and must own it:
   ```bash
   mkdir -p data && sudo chown 1001 data
   ```
   (Use the `ALCHEMIST_DATA_DIR` path instead if you changed it.) Skipped it? `alchemist-service` keeps restarting and `docker compose logs alchemist-service` shows `[SQLITE_CANTOPEN] Unable to open the database file`: run the command above, then `docker compose restart alchemist-service`.
4. Start:
   ```bash
   docker compose up -d --build
   ```
5. Check: `docker compose ps` shows both services running, and `curl http://localhost:2500/items` returns the base items.

## Reverse proxy (Nginx Proxy Manager)
Proxy Host: your domain, scheme `http`, forward host = the server's LAN IP (not `localhost`: inside NPM's container that is NPM itself), port `2500`. SSL: same certificate setup as your other services, Force SSL on.

## Day to day
| Task | Command |
|---|---|
| Update | `git pull && docker compose up -d --build` |
| Logs | `docker compose logs -f alchemist-service` |
| Stop | `docker compose down` (data stays in `./data`) |
| Backup | `docker compose stop alchemist-service`, copy `data/alchemist.db`, `docker compose start alchemist-service` |

Containers restart on their own after a crash or reboot (`restart: unless-stopped`).

## Observability
- Logs: `docker compose logs -f` (all services) or `docker compose logs -f ai-service`. Each line shows `[service] [thread] [traceId-spanId]` (blank outside a request).
- Traces: Zipkin UI at `http://<server>:2503`. Paste a `traceId` from the logs (the part before the `-`) into the search box to see one request across both services.
- Health: `docker compose ps` shows `healthy`/`unhealthy`; for details, `curl http://localhost:2500/actuator/health`. Metrics: `/actuator/metrics`. `ai-service` is internal only, so reach its actuator through `docker compose exec`.

## Access
WireGuard only. There are no accounts until [phase 5](PLAN.md#phase-5--accounts-and-per-user-collections), so anyone who can reach the URL can combine items and spend the AI credits: don't expose it to the internet.
