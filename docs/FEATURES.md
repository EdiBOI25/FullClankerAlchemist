# Functional features

- [ ] User starts with 4 items: water, air, fire and earth
- [ ] User is able to merge two items via AI chat
- [ ] *(Optional, if website is needed)* User is able to merge two items via *drag and drop*
- [ ] AI decides the result of the merge
- [ ] Merging the same pair always gives the same result (A + B = B + A)
- [ ] User can only merge items from their collection (an item can be merged with itself)
- [ ] Each item has one or more tags and an icon
- [ ] User is able to find items in their collection via tag(s) or by meaning (e.g. "something hot" -> fire, lava)

# Non-functional features

- [ ] Allow for "unreal" items - fantasy, sci-fi etc.
- [ ] AI is called only for new pairs; known pairs come from the DB
- [ ] No duplicate items (case-insensitive names); AI reuses similar existing items
- [ ] If the icon service is down, the merge still works (emoji fallback)
- [ ] API keys come from environment variables, never committed
- [ ] Data survives app restarts
- [ ] AI result is validated before saving (name + at least one tag); invalid -> retry once, then error
- [ ] Vector store can be rebuilt from the DB if its file is lost

# Project Requirements

- [ ] MCP server
- [ ] RAG + VectorStore: items stored as embeddings, used for semantic search and for reusing similar items when merging
  - [ ] File-based VectorStore (vectors saved to a JSON file next to the DB)
  - [ ] Local embedding model all-MiniLM-L6-v2 (~90 MB, runs on CPU, no API key) - *do some research on this. check if there are better/smaller/newer models*
- [ ] `/chat` endpoint (for Postman and more)
- [ ] Database (SQLite should be fine)
- [ ] REST API
- [ ] *(Optional, but may be required)* Docker container (DB + vector file on a mounted volume, embedding model cached in the image)

**Not required**:
- accounts (login/logout, auth etc.)
