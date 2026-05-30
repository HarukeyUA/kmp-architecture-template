# 0009. Auth: credential/session seam, opaque session, owned in-server

- Status: Accepted
- Date: 2026-05-30

## Context

Auth is universal but highly app-specific (anonymous, email/password, OAuth, magic-link, device-keys, …). A template can't hardcode one scheme. The developer is not a backend specialist and should not hand-roll the dangerous parts, and self-containment (own your server) is an explicit project goal.

## Decision

Separate the **Credential module** (how you prove identity — *varies*, swappable) from the **session/authorization infra** (*invariant*):

- **Invariant infra → `:server:core:auth`**: the `Principal` type, the Ktor `Authentication` middleware, the `authenticate { }` wrapper, session issuance/revocation, and a session-store interface.
- **Swappable front-end → `:server:feature:auth`**: a Credential module that verifies a credential and issues a Session.

**Default scheme:** an opaque **Session** token + a cached server-side store (Postgres `sessions` table + a Caffeine cache, generalizing witchy's device-auth cache). Revocation is a row delete. **Owned in-server** — no external auth provider.

Baked-in, non-negotiable defaults: **Argon2id** password hashing (never SHA), **platform-secure client token storage** (Keychain / Keystore-backed / encrypted, never plain DataStore — a Session is a credential, not a setting), and a global **401 → clear session → route to Login** interceptor wired into the existing Splash → Login → Main flow.

## Considered options

- **Hand-rolled stateless JWT + refresh-token rotation** — rejected as the default. Rotation/reuse-detection is the most error-prone part of DIY auth; the stateless-scale win is irrelevant at this scale, and revocation forces server state anyway. A short-JWT-access + opaque-refresh hybrid is kept as a documented upgrade path.
- **External managed auth provider** — rejected. Reintroduces the external dependency that self-containment is meant to escape. Footgun-minimization (opaque session + Argon2id baked in) is the alternative mitigation.

## Consequences

- witchy's device-key auth becomes "just another Credential module" — which validates the abstraction.
- On multi-node, Session revocation propagates within the cache TTL (bounded staleness — see ADR-0010); the cache is behind an interface so Redis can replace it without touching call sites.
