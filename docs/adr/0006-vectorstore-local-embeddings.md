# 6. File-based vector store with local embeddings

**Status:** Proposed (pending the embedding model research)

## Context
RAG and a vector store are required. They are used to search items by meaning and to show the AI similar existing items when combining. Anthropic has no embedding model. The dev laptop has 8 GB RAM and an i3 CPU.

## Decision
- Spring AI `SimpleVectorStore`, saved as a JSON file next to the DB.
- Local ONNX embedding model, running on the CPU. Candidate: `all-MiniLM-L6-v2` (~90 MB).
- Each item is embedded as `"name: tag1, tag2"` with `itemId` as metadata. The file is rebuilt from the DB if missing.

## Consequences
- No extra server, no API key, no cost; low RAM use.
- The model is downloaded on first run (~90 MB) and must be cached in the Docker image.
- The in-memory store fits thousands of items, not millions.
