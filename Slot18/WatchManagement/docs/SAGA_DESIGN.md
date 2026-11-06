# Watch Management Saga design

This assignment models `BrandWatches` as the Brand Service's own list of watches assigned to each brand. The Word assignment requests updates to related microservices without naming the specific data; `Assignment_Readme.md` sections 1.2 and 2.3 define this additional projection. The authoritative brand names live in `WatchBrands`, and the Watch Service stores a numeric `BrandID` reference without a cross-database foreign key.

The Watch Service orchestrates synchronous Saga steps. A step commits inside its owning database transaction. Saga status and step logs are stored in `DBWatch` separately. The Brand Service exposes idempotent participant operations under `/internal/brands/**`; the Gateway does not route those paths. A separate internal API key protects direct participant calls. The Gateway and data services verify access JWTs using the same signing key.

| Operation | First local transaction | Compensation | Second local transaction | Compensation |
| --- | --- | --- | --- | --- |
| Create | Insert `Watches` row in DBWatch | Delete inserted row | Assign `BrandWatches` in DBBrand | Unassign watch |
| Update | Save new `Watches` fields in DBWatch | Restore snapshot | Assign watch to requested brand in DBBrand | Assign previous brand |
| Delete | Unassign watch from brand in DBBrand | Reassign previous brand | Delete `Watches` row in DBWatch | None: final pivot |

For create and update, compensation runs in reverse order. A remote timeout may occur after DBBrand commits, so the orchestrator schedules the participant compensation **before** making the remote call. Each participant call carries a Saga UUID and phase. `SagaOperations` in DBBrand records an operation or a compensation tombstone in the same transaction as `BrandWatches`. This makes retries idempotent and prevents a delayed forward call from reapplying a compensated change.

For delete, the final hard delete and `COMPLETED` status share one DBWatch transaction. A failure after this pivot cannot restore the same identity reliably. A fault injected after the pivot is recorded as skipped and the Saga remains completed.

If a compensation exhausts three retries, the Saga remains `COMPENSATING` and further writes are rejected with HTTP 409. An administrator can query `GET /api/sagas/{id}` and retry the inverse steps with `POST /api/sagas/{id}/recover` once Brand Service is available. Recovery uses the stored snapshot and idempotent participant operations. It is safe to retry the recovery endpoint. The integration test covers a Brand Service outage and subsequent recovery.

A successful client response means the Saga has reached `COMPLETED`. A failed response includes `X-Saga-Id` and a `sagaStatus` value. A `COMPENSATING` status means manual retry is still needed. Demo fault injection is disabled by default and can be enabled only for local tests with `SAGA_FAULT_INJECTION=true`.

The current implementation serializes write requests in a single Watch Service process and checks persistent unfinished Saga states before starting another. Running multiple Watch Service replicas concurrently needs a shared lock or queue. Client retries of a create request are not deduplicated across separate Saga IDs; clients should inspect `X-Saga-Id` before retrying after an uncertain response. Access JWTs remain valid until expiry if an account is disabled after issue. The internal API key and service traffic should be protected by a private network or TLS outside a local demo.
