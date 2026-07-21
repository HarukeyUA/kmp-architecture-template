# Architecture Decision Records

Decisions for extending `kmp-template` into a fullstack client + server monorepo. See `CONTEXT.md` for the vocabulary and `ARCHITECTURE_SERVER.md` for the detailed structure. These describe a **target design** that is not yet implemented.

| ADR | Decision | Status |
|-----|----------|--------|
| [0001](0001-three-umbrella-structure.md) | Three-umbrella module structure joined by a shared seam | Accepted |
| [0002](0002-rest-ktor-resources-contract.md) | REST over Ktor Resources as the contract (reject kotlinx-rpc) | Accepted |
| [0003](0003-share-the-wire-not-the-domain.md) | Share the wire, never the domain | Accepted |
| [0004](0004-validation-split.md) | Validation split: pure shape shared, stateful server-only | Accepted |
| [0005](0005-error-model.md) | Error model — polymorphic `ApiError` + forward-compat fallback | Accepted — deferred grouping resolved by 0011 |
| [0006](0006-server-public-impl-internals.md) | Server internals mirror the client's public/impl split | Accepted |
| [0007](0007-persistence-and-migrations.md) | Exposed + Postgres + Flyway, timestamp migrations + drift test | Accepted |
| [0008](0008-metro-di-everywhere.md) | Metro DI across the whole monorepo | Accepted |
| [0009](0009-auth-credential-session-seam.md) | Auth: credential/session seam, opaque session, owned in-server | Accepted |
| [0010](0010-scale-posture.md) | Scale posture: don't foreclose horizontal scaling | Accepted |
| [0011](0011-per-endpoint-declared-errors.md) | Per-endpoint Declared errors — `Endpoint` grows `Err`, twin Declared/Ambient wrappers | Accepted |
