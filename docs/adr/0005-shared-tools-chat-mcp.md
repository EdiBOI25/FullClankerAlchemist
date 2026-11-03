# 5. One set of tools for chat and MCP

**Status:** Superseded by [ADR-0010](0010-microservices.md)

## Context
The `/chat` endpoint and the MCP server need the same actions: list items, combine, search.

## Decision
One `GameTools` class in `adapter.in` with Spring AI `@Tool` methods, registered for both the chat client and the MCP server. The tools only call inbound ports.

## Consequences
- One implementation; chat and MCP behave the same.
- Tool descriptions must make sense for both.
