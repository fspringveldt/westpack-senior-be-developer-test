# Interview Notes

This project is a deliberately scoped engineering exercise. It demonstrates good API design, defensive coding, and operational thinking while keeping the business scope intentionally narrow enough for a focused interview or code review.

## Five topics to be ready to explain

### 1. Customer-limit enforcement and concurrency

The core logic sits in `AccountService.createAccount()`.

- It checks `countByCustomerNameIgnoreCase(customerName)`
- It rejects the request when the count reaches the configured limit
- The code comments explicitly note that a production system would use a persistent customer identifier instead of a customer name

This is an important discussion point because the current implementation is intentionally simplified for the exercise. It is good enough for a small service, but it is not robust enough for real customer identity management. The database-side enforcement and uniqueness constraints are what make the limit check acceptable under concurrent requests, while the comment in the service explicitly calls out the production gap.

### 2. UUIDs for internal and external identifiers

The `Account` entity uses:

- `id` as the internal database identifier
- `accountNumber` as the customer-facing identifier

Both are generated as UUIDs.

This is a sensible design choice because it avoids predictable IDs and keeps internal and external identifiers separate. It is also easy to explain in an interview: the system avoids exposing internal DB identity as the public contract while keeping a clean separation of responsibilities.

### 3. Cache-aside behavior and resilience

The service uses `@Cacheable` and `@CachePut` for account lookups and creation.

This is a cache-aside pattern:

- read from Redis when available
- fall back to PostgreSQL on cache misses or cache failure
- write the result back to cache after successful reads or writes

This is a good engineering trade-off for a small API because it improves read latency without making the service dependent on Redis availability. The important trade-off is eventual consistency: cached data can become stale, which is acceptable for this use case but should be recognized explicitly.

### 4. Offensive nickname filtering

`OffensiveNicknamePolicy` normalizes the nickname before comparison:

- lowercases the value
- strips non-alphanumeric characters
- checks whether the normalized value contains a known offensive term

This is a practical anti-evasion technique and catches variants such as `B_a_d Word` matching `badword`.

The limitation is that this is still a rule-based heuristic. It is serviceable for an interview exercise, but it is not a complete moderation system for production. A production service would likely use a more explicit moderation policy, better false-positive controls, and a dedicated backend capability.

### 5. Standardized error handling

`GlobalExceptionHandler` translates application and infrastructure failures into RFC 7807 `ProblemDetail` responses.

This gives the API a consistent shape for:

- validation failures
- malformed requests
- offensive names
- limit breaches
- not found cases
- transient database outages

This is valuable because it keeps API behavior predictable for clients and reduces the chance of leaking inconsistent internal errors into the public contract.

## The most likely interview gotcha

The most important production gap is the use of customer name as the identity primitive instead of a dedicated customer record.

This is deliberate for the exercise, but it is the principal architectural simplification to call out. In a real system, the safer design would be:

- create a `Customer` entity with a stable `customer_id`
- add a foreign key from `Account` to `Customer`
- replace name-based counting with `customer_id`-based counting
- add auth, audit logging, and a fuller account lifecycle model

This matters because the current approach breaks down in real-world scenarios such as:

- duplicate names
- customer name changes
- identity verification and access control
- compliance and audit requirements
- multi-tenant or multi-organization scenarios

The right interview framing is not to pretend this is a production-grade customer model, but to acknowledge the simplification and explain the correct direction for the real system without losing the scope discipline of the exercise.

## Summary

This project is best framed as a clean, disciplined engineering prototype: it demonstrates good API structure, validation, resilient caching, and clear operational thinking without claiming to be a complete production platform. The strongest answers are the ones that explain the trade-offs honestly and show awareness of what would need to change for real-world deployment.
