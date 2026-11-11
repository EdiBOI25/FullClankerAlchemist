# 13. Security details

**Status:** Accepted (amends the security details in [ADR-0011](0011-accounts-and-per-user-collections.md))

## Context
ADR-0011 chose accounts with a session cookie plus HTTP Basic, but left CSRF and the details open.

## Decision
- Endpoints: `POST /auth/signup`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`. Signup doesn't log in.
- Login starts a session (cookie, kept in memory) and changes the session id; nothing else creates a session. HTTP Basic is accepted on every endpoint too, without a session.
- CSRF protection is off. The session cookie is `SameSite=Strict`.
- The app runs plain HTTP behind a TLS reverse proxy ([Nginx Proxy Manager](../SELF-HOSTING.md)) and trusts its forwarded headers (`server.forward-headers-strategy=native`), so requests that reached the proxy over HTTPS count as HTTPS: the session cookie gets `Secure` and Spring Security adds HSTS. Plain HTTP on the LAN keeps working, without either.
- 401 and 403 are ProblemDetail JSON. A wrong username and a wrong password get the same message.
- Usernames: 3–30 characters `[a-zA-Z0-9_-]`, stored as typed, unique ignoring case; login ignores the case too. A taken name → 409. The name check and the insert run in one transaction, so concurrent signups queue ([ADR-0012](0012-sqlite-immediate-transactions.md)) and the second sees the first one's row.
- Passwords: at least 8 characters and at most 72 bytes in UTF-8 (bcrypt's limit: 72 ASCII characters, fewer with accents or emoji), stored as a bcrypt hash. Longer ones get a 400.
- Admin from `ADMIN_USERNAME` and `ADMIN_PASSWORD`, at startup after the base items:
  - both empty → no admin
  - created if missing; its password is updated from `.env` at startup if it changed
  - name taken by a normal user → warning, that user is left unchanged
  - only one set, or an invalid value → startup fails
  - a new `ADMIN_USERNAME` adds a second admin while the old one keeps working
- Access: public = signup, login, `/actuator/health`; admin = `/actuator/metrics`, `/actuator/info`; everything else needs login. The servlet error dispatch to Boot's `/error` is permitted, so errors keep their status (404, 415…) instead of becoming 401. `ai-service` has no auth; compose doesn't publish it.

## Consequences
- A restart logs everyone out; Basic clients don't notice.
- CSRF: writes take a JSON body (Spring answers other content types with 415) or use `DELETE`; another origin can't send either without a CORS preflight, and none is allowed. `SameSite=Strict` also keeps the session cookie off other sites' requests, which covers the body-less logout; only a page on the same site (a sibling subdomain, or another port on the same host) could send that, and at worst it logs the user out. Turn CSRF on if HTML forms, CORS or another POST without a JSON body ever come.
- Plain HTTP sends Basic passwords, login bodies and the session cookie unencrypted; anyone who can watch that traffic can read them. Compose publishes 2500 on every interface, so the app is reachable without the proxy: use HTTPS wherever the network isn't yours.
- Forwarded headers can be faked: NPM passes a client's own `X-Forwarded-Proto` and `X-Forwarded-Host` through, and Boot trusts them from any internal address (`server.tomcat.remoteip.internal-proxies`: private, loopback, link-local and CGNAT ranges); a machine on the LAN can fake its client IP too. They only change that client's own request; no access decision uses them.
- The generic 401 hides whether a username exists, but signup's 409 still reveals it. Accepted.
- Not planned: password change, account deletion, promoting users, rate limiting, closed signup. So:
  - a leaked password or an abusive account needs a manual DB edit;
  - login guesses are only slowed by bcrypt, and every request with a Basic header costs one bcrypt check, so a flood can max out the CPU;
  - anyone who can reach the app can sign up and spend AI credits.
