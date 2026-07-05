# Fullstack KMP Template

The shared vocabulary for a Kotlin Multiplatform monorepo where a Compose Multiplatform client and a Ktor server are joined by a shared contract, so the two halves feel like "one whole thing." This file is a glossary only — see `docs/adr/` for decisions and `ARCHITECTURE_SERVER.md` for structure.

## Language

**Seam**:
The shared contract layer that both client and server depend on, and where cross-stack unity is delivered.
_Avoid_: glue, bridge, shared layer

**Umbrella**:
A top-level module group (`:client`, `:shared`, `:server`) whose name encodes what it is allowed to depend on.
_Avoid_: root module, top-level group

**Contract**:
The wire-level definition of an API surface — routes, DTOs, shape validation, and errors — living in the Seam.
_Avoid_: API, schema, interface

**Domain**:
A vertical slice of server functionality owned end-to-end (e.g. notes, account), spanning a Seam contract module and a server slice.
_Avoid_: feature (a client-only term), service, bounded context

**Wire** (a.k.a. **DTO**):
The serialized over-the-network representation of data or errors; the only thing the Seam holds.
_Avoid_: payload, entity, bare "model"

**Domain model**:
A side's own internal representation of a concept, deliberately distinct from the Wire; client and server each own theirs.
_Avoid_: entity, DTO

**Credential module**:
A swappable server component that verifies a proof of identity (password, OAuth, device key) and issues a Session plus its first Access token.
_Avoid_: login, auth provider

**Session**:
An opaque, server-validated token that represents an authenticated Principal and is revoked by deletion. Presented only to the refresh and logout endpoints, where it mints Access tokens (ADR-0009 as amended).
_Avoid_: bare "token", refresh token (in prose — the wire field is `refreshToken`), cookie

**Access token**:
A short-lived, signed JWT minted from a Session and carried on every authenticated request; verified statelessly, so it cannot be revoked — it just expires. Revoking the Session stops new ones from being minted.
_Avoid_: bare "token", bare "JWT", bearer

**Principal**:
The authenticated identity attached to a request once its Access token is verified.
_Avoid_: bare "user", bare "account", subject

**Blob** (via **BlobStore**):
A binary object (image, file) kept in S3-compatible object storage under an opaque key — **never on an instance's local disk**. The `BlobStore` is the interface; the client transfers bytes **directly** to/from storage through short-lived presigned URLs, so blobs never stream through the app.
_Avoid_: file, attachment, upload (for the stored object)

**Scheduled job**:
A periodic, **idempotent** unit of background work that never runs concurrently on two instances, coordinated by a Postgres advisory lock. The lock bounds concurrency, not frequency — idempotency is what makes cross-node re-runs safe.
_Avoid_: cron, task, worker, sweeper (use for the specific job, not the concept)

## Relationships

- A **Contract** lives in the **Seam**; both **Client** and **Server** depend on it, never on each other.
- A **Domain** = one **Seam** contract module + one **Server** slice (+ the **Client** features that consume it).
- **Client** and **Server** each map the **Wire** to their own **Domain model** — the Wire is shared, the Domain model is not.
- A **Credential module** issues a **Session**; a **Session** mints **Access tokens**; a verified **Access token** yields a **Principal**.

## Example dialogue

> **Architect:** "We're adding image sync. Does that mean a new **Domain**?"
> **Developer:** "Yes — a `:shared:images` **Contract** and a `:server:feature:images` slice. The client's existing media **feature** consumes the **Contract**."
> **Architect:** "And the upload auth?"
> **Developer:** "Same **Access token** as everything else — the **Principal** is already on the request. Images is a **Domain**, not a new **Credential module**. We only add one of those to change *how you log in*, not *what you can do*."

## Flagged ambiguities

- **"feature" vs "domain"** — On the client a *feature* is a UI slice (`:client:feature:*`). On the server the equivalent functional slice is a **Domain**. The server module path is `:server:feature:<domain>` only for symmetry with the client; the *concept* is **Domain**. Say "Domain" when talking about server slices.
- **"token" → Session / Access token** — "token" is overloaded (session token, access token, CSRF token, push token). The revocable credential is a **Session**, the per-request JWT is an **Access token**; avoid bare "token."
- **"model" → Wire / Domain model / UI model** — three distinct things (UI/Domain/Wire mapping is already established in `ARCHITECTURE.md`): the **Wire** (DTO) crosses the network, the **Domain model** is a side's internal type, the *UI model* is render-optimized. Never say bare "model."
- **"account" / "user" → Principal** — when referring to the authenticated caller, say **Principal**; reserve "account"/"user" for concepts inside a specific **Domain**.
