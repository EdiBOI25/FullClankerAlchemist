# 9. Pluggable chat provider

**Status:** Accepted

## Context
Combining needs an LLM. Gemini's API has a free tier, so every teammate can run the app; Anthropic stays available. Spring AI has one starter per provider and picks the chat model with `spring.ai.model.chat`.

## Decision
- Both starters are on the classpath; `AI_PROVIDER` in `.env` (mapped to `spring.ai.model.chat`) picks one, `google-genai` by default.
- Adapters use Spring AI's `ChatClient`, never a provider SDK. `config` may build a provider client only for settings the starter lacks (`config.GeminiClientConfig` for the Gemini timeout).
- No sampling options (`temperature`, `top_p`, `top_k`); current Claude models reject them.
- AI calls time out after `ai.timeout` (60 s).
- Fail fast: `config.AiProviderCheck` stops startup when `AI_PROVIDER` is blank or unknown, or the selected provider has no key. The provider is never switched automatically.
- Default tests pin Gemini with a dummy key and never call the AI; live tests run only with `LIVE_TESTS=true` and a key.

## Consequences
- Switching provider is a `.env` change, not a code change.
- Running the app needs a key, even for read-only endpoints.
- A new provider needs its starter, an `AiProviderCheck` entry, its key and model lines in `application.properties`, a timeout (a starter property, or a client in `config` if the starter has none), and its key in `.env.example`.
- Vertex AI (project and location instead of a key) is not supported.
- Gemini's timeout covers the whole call, SDK retries included (plus up to ~30 s backoff); Anthropic's applies per attempt (2 retries, so up to ~3 min).
- Embeddings stay local ([ADR-0006](0006-vectorstore-local-embeddings.md)), independent of the chat provider.
