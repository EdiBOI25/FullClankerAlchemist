# Functional features

- [x] User starts with 4 items: water, air, fire and earth
- [ ] User can sign up, log in and log out
- [ ] User is able to merge two items via REST and via AI chat
- [ ] *(Optional, if website is needed)* User is able to merge two items via *drag and drop*
- [ ] AI decides the result of the merge
- [ ] Merging the same pair always gives the same result (A + B = B + A)
- [ ] User can only merge items from their collection (an item can be merged with itself)
- [ ] Each user has their own collection; items and recipes are shared, so a pair is generated once for everyone
- [ ] User sees their own items and recipes by default, and can ask for everyone's (e.g. "what did others discover with iron?")
- [ ] Each item and recipe shows who discovered it first
- [ ] A merge says whether the item is new for the user and whether it is a first discovery (new for everyone)
- [ ] Each item has one or more tags and an emoji; an Iconify icon is optional
- [ ] User is able to find items via tag(s) or by meaning (e.g. "something hot" -> fire, lava), in their collection or everyone's
- [ ] Chat remembers the conversation and answers in a chosen tone or role
- [ ] Admin can delete a bad recipe

# Non-functional features

- [ ] Allow for "unreal" items - fantasy, sci-fi etc.
- [x] AI is called only for new pairs; known pairs come from the DB, whoever tried them first
- [ ] No duplicate items (case-insensitive names); AI reuses similar existing items
- [ ] If the AI service is down, known pairs still merge; if Chroma is down, merging works without similar items
- [ ] If the icon service is down, the merge still works (emoji fallback)
- [x] API keys come from environment variables, never committed
- [x] Data survives app restarts
- [x] AI result is validated before saving (name, emoji, tags); invalid or failed -> retry once, then error
- [ ] Vector store can be rebuilt from the DB if it is lost
- [ ] Every input is validated where it enters a service: REST requests, AI results, calls between services, entities
- [ ] Every behaviour is tested, failure paths included; AI generation and chat are tested with a mocked model, live, and judged by an AI evaluator
- [ ] Observability: health and metrics in every service, one trace per request across services (Zipkin), trace id in the logs
- [ ] Resilience: a timeout on every outbound call, retries that don't stack, fallbacks

# Project requirements

From the teacher, see [teacher-requirements.txt](teacher-requirements.txt):

- [ ] **Mark 5:** Spring AI with at least one AI service; microservices architecture ([ADR-0010](adr/0010-microservices.md))
- [ ] **Mark 6:** validation of at least 10 fields (we validate every input), at least 10 unit tests with at least one mocked dependency, Docker with a `compose.yaml` running several containers
- [ ] **Mark 7:** context-aware, parameterized AI: chat remembers previous messages and answers in a chosen tone or role
- [ ] **Mark 8:** RAG pipeline with Spring AI and Chroma
- [ ] **Mark 9:** Spring Security: sign up, login, logout, roles ([ADR-0011](adr/0011-accounts-and-per-user-collections.md))
- [ ] **Mark 10:** MCP server

Also asked for by the teacher: observability, resilience, tests of the AI generation and the chat.
