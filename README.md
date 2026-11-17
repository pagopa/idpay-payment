# idpay-payment

## Transaction test support

The internal test-support endpoint is disabled by default. Enable it only in test
environments with `app.test-support.enabled=true` (environment variable
`APP_TEST_SUPPORT_ENABLED=true`).

`PATCH /idpay/internal/test-support/transactions/{id}/end-date` updates only
`trxEndDate` for the transaction matching the path variable `id` and the required
query parameters `initiativeId` and `userId`. The required `newEndDate` query
parameter accepts an ISO-8601 timestamp with
an offset, for example `2026-11-20T23:59:59+01:00`. When constructing a query
string, URL-encode the `+` as `%2B`.

The endpoint returns `204 No Content` when a transaction matches (including when
the date is unchanged), `404 Not Found` when none matches, and `400 Bad Request`
for missing parameters or an invalid timestamp. It does not publish events,
reschedule timeouts, or change the transaction status.

## How test helm template

```sh
 helm dep build && helm template . -f values-dev.yaml  --debug
```

## How deploy helm

```sh
helm dep build && helm upgrade --namespace idpay --install --values values-dev.yaml --wait --timeout 5m0s idpay-payment .
```
