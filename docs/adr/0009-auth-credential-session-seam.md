# 0009. Auth: credential/session seam, opaque session, owned in-server

- Status: Accepted (amended 2026-07-04: the documented JWT upgrade path implemented — JWT access +
  opaque refresh hybrid, via Ktor's native `jwt()` provider)
- Date: 2026-05-30

## Context

Auth is universal but highly app-specific (anonymous, email/password, OAuth, magic-link, device-keys, …). A template can't hardcode one scheme. The developer is not a backend specialist and should not hand-roll the dangerous parts, and self-containment (own your server) is an explicit project goal.

## Decision

Separate the **Credential module** (how you prove identity — *varies*, swappable) from the **session/authorization infra** (*invariant*):

- **Invariant infra → `:server:core:auth`**: the `Principal` type, the Ktor `Authentication` middleware (the `jwt()` provider), the `authenticate { }` wrapper, access-token minting (`AccessTokenIssuer`), session issuance/revocation, and a session-store interface.
- **Swappable front-end → `:server:feature:auth`**: a Credential module that verifies a credential and issues the token pair (access token + Session).

**Default scheme** (amended 2026-07-04 — the hybrid kept below as the upgrade path is now the implementation): a short-lived **JWT access token** (HS256, Ktor's native `jwt()` provider, ~15 min TTL) carried on every authenticated request and verified statelessly, plus the opaque **Session** as the refresh credential — Postgres `sessions` table + a Caffeine cache (generalizing witchy's device-auth cache), consulted only by `POST /v1/auth/refresh` and revoked by row delete. There is deliberately **no refresh-token rotation**: rotation/reuse-detection was the error-prone part the original decision refused to hand-roll, and the Session already has its own TTL and revocation story. Revoking a Session cuts off *minting*; an already-issued access token stays valid until its TTL — bounded staleness, same shape as the cache TTL in ADR-0010. **Owned in-server** — no external auth provider.

Baked-in, non-negotiable defaults: **Argon2id** password hashing (never SHA), **platform-secure client token storage** (Keychain / Keystore-backed / encrypted, never plain DataStore — both tokens are credentials, not settings), and the client's Ktor-native bearer provider: attach access token → on 401 refresh through `/v1/auth/refresh` → **if the refresh itself is rejected, clear session → route to Login**, wired into the existing Splash → Login → Main flow. `JWT_SECRET` is required (and length-checked) in production; dev gets a localhost default like every other secret.

## Considered options

- **Hand-rolled stateless JWT + refresh-token rotation** — rejected as the default. Rotation/reuse-detection is the most error-prone part of DIY auth; the stateless-scale win is irrelevant at this scale, and revocation forces server state anyway. A short-JWT-access + opaque-refresh hybrid was kept as a documented upgrade path — and adopted by the 2026-07-04 amendment (without rotation, so the error-prone part stays out).
- **Pure stateless JWT (no server-side session)** — rejected at amendment time. Losing revocation entirely (logout, password change, compromise) is not acceptable even for a template; keeping the Session as the refresh credential preserves it at the cost of one indexed lookup per TTL window.
- **External managed auth provider** — rejected. Reintroduces the external dependency that self-containment is meant to escape. Footgun-minimization (opaque session + Argon2id baked in) is the alternative mitigation.

## Consequences

- witchy's device-key auth becomes "just another Credential module" — which validates the abstraction.
- On multi-node, Session revocation propagates within the cache TTL (bounded staleness — see ADR-0010); the cache is behind an interface so Redis can replace it without touching call sites.
- (2026-07-04) Authenticated requests no longer touch the session store — it is consulted once per access-token TTL, on refresh. The flip side: revocation is now bounded by `JWT_ACCESS_TTL_MINUTES` on *every* node, not just remote ones; a deployment that needs instant kill switches should shorten the TTL rather than reintroduce per-request lookups.
- (2026-07-04) Logout names the refresh token in its body — a stateless bearer JWT can't identify the Session to revoke.
