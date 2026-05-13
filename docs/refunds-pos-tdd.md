# POS Refunds — Phase 0 Technical Design

## Goal

Let merchant staff mark a previously-succeeded payment as refunded from the
POS device. Matches the Phase 0 scope of the
[MX Technical Feasibility doc](https://walletconnect.notion.site/Refunds-MX-Technical-Feasibility-3353a661771e806f8a7adc0ae34f11cf):
no on-chain execution, no Merchant Refund Account, no partial refunds.

## API contract (`2026-02-18`)

Both refund endpoints live on **`api.merchant.pay.walletconnect.com`** — a
different host from the existing `api.pay.walletconnect.com` the SDK uses for
payment creation, status polling, cancel, and the legacy `/v1/merchants/payments`
history route. The SDK keeps its existing `PayApi` (core host) and adds a
dedicated `MerchantApi` (merchant host) that shares the same auth header
interceptor.

- `GET /v1/payments?referenceId=<3–35 chars>` — substring search for staff
  lookup. Same `Api-Key`, `Merchant-Id`, `WCP-Version` headers as the rest of
  the SDK. Standard `limit`, `cursor`, `status`, `startTs`, `endTs`, `sortBy`,
  `sortDir`. Response shape identical to today's `/v1/merchants/payments`.
- `POST /v1/refunds` — body `{ "paymentId": "pay_…" }`, no amount. Response
  `{ "paymentId": "…" }`. Errors:
  - `400 params_validation` — bad input.
  - `400 payment_not_succeeded` — not refundable.
  - `404 not_found` — unknown payment.
  - `409 already_refunded` — idempotent retry; treat as soft success.
  - `401` auth family / `500` / `502` — surfaced as transient.

Note: the published payment-list response does **not** yet expose a "refunded"
substatus. The sample app keeps an optimistic `isRefunded` flag locally; once
the backend ships the substatus the SDK's mapper fills it from the wire.

## SDK surface (`com.walletconnect.pos`)

```kotlin
// PosClient
suspend fun searchPaymentsByReference(
    referenceId: String,                   // 3..35, ASCII letters/digits + "/-:.,+ "
    limit: Int = 20, cursor: String? = null,
    statuses: List<Pos.TransactionStatus>? = null,
    dateRange: Pos.DateRange? = null,
): Result<Pos.TransactionHistoryResult>

suspend fun refundPayment(paymentId: String): Result<Pos.RefundResult>

// Pos
data class RefundResult(val paymentId: String)
sealed interface RefundError {
    data class PaymentNotFound(val message: String) : RefundError
    data class AlreadyRefunded(val message: String) : RefundError
    data class PaymentNotSucceeded(val message: String) : RefundError
    data class InvalidParams(val message: String) : RefundError
    data class Unauthorized(val message: String) : RefundError
    data class Network(val message: String) : RefundError
    data class Unknown(val code: String, val message: String) : RefundError
}
class RefundException(val error: RefundError) : Exception(error.message)
data class Transaction(
    /* …existing fields… */
    val isRefunded: Boolean = false,
    val refundedAt: String? = null,
)
```

Failures from `refundPayment` are returned via
`Result.failure(RefundException(error))` so callers can do exhaustive `when`
on `Pos.RefundError`. Pulse events:
`trackRefundInitiated / trackRefundSucceeded / trackRefundFailed`.

## Sample app flow

Refund is initiated from the existing Activity screen — no new top-level entry
point.

1. Home → **Activity**.
2. Activity gains a search bar above the filter row. Typing 3+ chars triggers
   `searchPaymentsByReference` (300 ms debounce). Status / date filters and
   the stats summary hide while a search is active. Pagination via `cursor`
   reuses `loadMoreTransactions()`.
3. Tap a result → existing `TransactionDetailContent` bottom sheet gains a
   primary **Refund** button. Visible only when the transaction is
   `SUCCEEDED` and not already refunded.
4. **Refund** swaps the sheet content (reusing the `activeSheet` enum) to a
   confirmation step showing amount, reference ID, and the Phase 0 caveat
   *"Funds are not returned to the buyer wallet automatically"*.
5. **Confirm refund** calls `PosClient.refundPayment`. Success: toast, sheet
   closes, the transaction in `loadedTransactions` is marked
   `isRefunded = true` and renders a `Refunded` `StatusBadge`.
   `already_refunded` (409) is treated as soft success with a different toast
   copy. Other errors stay inline in the sheet or surface as a snackbar with
   Retry.

UI is built with the existing `WCTheme.colors / spacing / borderRadius /
typography` tokens and XML vector drawables only (`ic_search.xml`,
`ic_refund.xml`).

## Out of scope

- Partial refunds (API has no amount field).
- On-chain settlement / Merchant Refund Account / refund queue (Phase 1).
- `GET /v1/refunds` listing (not in Phase 0 API).
- Receipt printing for refund.

## Open items

- Confirm `WCP-Version` pin (`2026-02-19.preview`) accepts the
  `2026-02-18` refund endpoint; otherwise add a per-call version override.
- Adopt the backend's "refunded" substatus on the payment record as soon as
  it ships — the mapper is forward-compatible.
