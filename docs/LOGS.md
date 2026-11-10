# Logs

## Where
| Running with | Command |
|---|---|
| Docker Compose, from the folder with `compose.yaml` | `docker compose logs -f` (all) or `docker compose logs -f alchemist-service` (one service) |
| Docker, from anywhere | `docker logs -f fullclanker-alchemist-alchemist-service-1` (container names: `docker ps`) |
| IntelliJ or `spring-boot:run` | the Run window or the terminal |

`docker compose logs` takes service names (`alchemist-service`, `ai-service`, `zipkin`). Add `--tail 100` or `--since 10m` to limit the output.

## Reading a line
```
2026-11-10T12:47:04.740+02:00  INFO 14252 --- [alchemist-service] [nio-2500-exec-1] [4bf92f3577b34da6a3ce929d0e0e4736-3ad96d29321d21a8] f.a.adapter.out.HttpItemGenerator : Generated item for fire + water -> Steam (duration=130 ms)
```
Time, level, process id, `[service]`, `[thread]`, `[traceId-spanId]` (blank outside a request), logger, message.

## Following one request
- The trace id is the part before the `-`. Every log message of that request, in both services, carries it (stack-trace lines don't, so grep shows only an exception's first line):
  `docker compose logs alchemist-service ai-service | grep 4bf92f3577b34da6a3ce929d0e0e4736`
- Zipkin (http://localhost:2503): paste the trace id into the search box to see the request across both services, with timings.
- To pick the trace id yourself, send a `traceparent: 00-<32 hex>-<16 hex>-01` header (e.g. in Postman).

## Lines worth knowing
| Line | Meaning |
|---|---|
| `Generated item for A + B -> X (duration=… ms)` | a new pair was generated (logged by both services) |
| `AI result generation failed for A + B; retrying` | the AI call failed (bad key, timeout, rate limit) or returned an invalid item; `ai-service` tries once more |
| `AI service unavailable for A + B; retrying` | `alchemist-service` can't reach `ai-service` and tries once more; if that fails too, the request returns 503 |
| `Dropped N spans due to ConnectException` | Zipkin isn't reachable; for local runs start it with `docker compose up -d zipkin` |
| `[SQLITE_CANTOPEN] Unable to open the database file` | the data folder isn't writable; see [SELF-HOSTING.md](SELF-HOSTING.md) step 3 |
