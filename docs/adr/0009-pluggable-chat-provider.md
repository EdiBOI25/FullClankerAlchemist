# 9. Pluggable chat provider

**Status:** Accepted

## Context
Combining needs an LLM. Gemini's API has a free tier, so every teammate can run the app; Anthropic stays available. Spring AI has one starter per provider and picks the chat model with `spring.ai.model.chat`.

## Decision
- Both starters are on the classpath; `AI_PROVIDER` in `.env` (mapped to `spring.ai.model.chat`) picks one, `google-genai` by default.
- Code uses Spring AI's `ChatClient`, never a provider SDK.
- Fail fast: `config.AiProviderCheck` stops startup when `AI_PROVIDER` is blank or unknown, or the selected provider has no key. The provider is never switched automatically.
- Tests pin Gemini with a dummy key and never call the AI.

## Consequences
- Switching provider is a `.env` change, not a code change.
- Running the app needs a key, even for read-only endpoints.
- A new provider needs its starter, an `AiProviderCheck` entry, and its key line in `application.properties` and `.env.example`.
- Vertex AI (project and location instead of a key) is not supported.
- Embeddings stay local ([ADR-0006](0006-vectorstore-local-embeddings.md)), independent of the chat provider.
