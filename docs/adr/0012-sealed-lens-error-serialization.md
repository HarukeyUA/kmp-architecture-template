# Sealed-lens error serialization with statuses on the errors

Amends the *mechanism* half of [ADR-0005](./0005-error-model.md) and the narrowing in
[ADR-0011](./0011-per-endpoint-declared-errors.md); the typed `Endpoint`/`ApiError` seam itself
stands. Ported from a downstream project that proved it in production.

Adding one Declared error used to touch ~5 places — the variant declaration, a hand-listed
`subclass()` line in the domain's `SerializersModule`, a status branch in the feature's
`ApiErrorStatusMapper`, and two goldens — with two silent failure modes: a missing `subclass()`
crashed the server at encode time, and a missing status branch answered 500, which severs the
client's Declared channel (it parses an `ErrorEnvelope` only out of a 4xx). We collapsed the
ritual to the declaration plus one deliberate golden edit:

- **Every Declared-error lens is a `@Serializable sealed interface`**, so the compiler generates a
  closed `SealedClassSerializer` (byte-identical wire JSON to the old polymorphic registry).
  `Endpoint.error` carries that lens as a `KSerializer<Err>` — mirroring the body serializers — and
  its constructor rejects a non-SEALED serializer, so forgetting `@Serializable` on a future lens
  fails at `*Api` class-load instead of silently degrading every Declared error to Ambient.
- **The client narrows by decoding**: try the endpoint's lens (`CallFailure.Declared`), fall back to
  the sealed `CommonApiError` set, then to `UnknownApiError(code, raw, status)` — the forward-compat
  behavior ADR-0005 required, now without the open-polymorphism registry and custom default
  deserializer. The unknown error carries the *actual* response status: the status line arrives
  independently of the body, so it is known even when the code isn't. `ErrorEnvelope.error` is raw
  JSON because narrowing needs the lens, which only the call site holds. A known-but-undeclared
  code (version skew) now surfaces as `UnknownApiError` rather than its concrete type; nothing
  matched on concrete domain types in the Ambient channel.
- **`ApiError` declares `val status: HttpStatusCode`** on each variant (as a getter — a backing
  field would be picked up by serialization). The status is part of the wire contract, so it lives
  at the declaration; the whole `ApiErrorStatusMapper` apparatus and per-feature status/module
  `@Provides` are deleted, and the seam `Json` is one static `seamJson` on both ends.

## Consequences

- Adding a Declared error = declare the variant (status included, compiler-enforced) + edit the
  domain's freeze golden (`operation → {code → status}`). No registration anywhere.
- Two guarantees the registry gave for free are now explicit tests: global code uniqueness
  (`UniqueErrorCodesTest` in `:server:app`, the one module that sees every domain) and the
  Declared ⇒ 4xx invariant (asserted in each domain's freeze test).
- The envelope encode/decode helpers (`encodeApiError` / `decodeDeclaredApiError` /
  `decodeAmbientApiError` in `ApiErrorWire.kt`) are hand-rolled where kotlinx polymorphism used to
  be — do not "fix" this back to a registered-module `Json`: the registry is exactly the multi-site
  ritual this ADR removes.
- A plain (unannotated) sealed interface resolves to an OPEN `PolymorphicSerializer` via
  `serializer<T>()` — the `Endpoint` constructor `require` is the tripwire for that trap.
- The old Kotlin/Native caveat (a bare `serializer<ApiError>()` can't resolve an interface
  serializer at runtime) is moot: nothing serializes a bare `ApiError` anymore — everything rides a
  compile-time-generated sealed lens.
