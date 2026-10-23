# 1. SQLite database

**Status:** Accepted

## Context
Small single-player game. Data: items, tags, recipes. It must run on a dev laptop and in Docker without extra services.

## Decision
Use a SQLite file (`alchemist.db`) through JPA/Hibernate (community SQLite dialect).

## Consequences
- No DB server, zero setup. The whole DB is one file, easy to reset or mount in Docker.
- One writer at a time. Fine for a single player.
- `*.db` is gitignored, so every developer has their own local data.
