# 9. Pluggable chat provider

**Status:** Accepted

## Context
Combining needs an LLM. Gemini's API has a free tier, so every teammate can run the app; Anthropic stays available. Spring AI has one starter per provider and picks the chat model with `spring.ai.model.chat`.

## Decision
- Both starters are on the classpath; `AI_PROVIDER` in `.env` (mapped to `spring.ai.model.chat`) picks one, `google-genai` by default.
- Code uses Spring AI's `ChatClient`, never a provider SDK. `config` may build a provider client only for settings the starter lacks (`config.GeminiClientConfig` for the Gemini timeout and retries).
- No sampling options (`temperature`, `top_p`, `top_k`); current Claude models reject them.
- AI calls time out after `ai.timeout` (30 s) and the provider SDKs never retry (`spring.ai.anthropic.max-retries=0`), so retries don't stack ([ADR-0010](0010-microservices.md)).
- Fail fast: `config.AiProviderCheck` stops startup when `AI_PROVIDER` is blank or unknown, or the selected provider has no key. The provider is never switched automatically.
- Default tests pin Gemini with a dummy key and never call the AI; live tests run only with `LIVE_TESTS=true` and a key.

## Consequences
- Switching provider is a `.env` change, not a code change.
- Running the app needs a key, even for read-only endpoints.
- A new provider needs its starter, an `AiProviderCheck` entry, its key and model lines in `application.properties`, a timeout and SDK retries turned off (starter properties, or a client in `config` if the starter has none), and its key in `.env.example`.
- Vertex AI (project and location instead of a key) is not supported.
- A call takes 1–3 s (5 live calls per provider, one 16 s outlier on Gemini), so 30 s leaves a wide margin. `ai-service`'s `ChatClientItemGenerator` retries once on a failure or an invalid result, so one generation takes at most ~60 s; `alchemist-service` waits up to 90 s (`ai-service.read-timeout`) and gives up connecting after 2 s.
- Without SDK retries, a rate-limited call (429, likely on Gemini's free tier) fails instead of being waited out; the user gets a 502 and can try again. Chat has no retry at all.
- Embeddings are independent of the chat provider; the model is chosen in the RAG phase ([ADR-0006](0006-vectorstore-local-embeddings.md) was rejected).
