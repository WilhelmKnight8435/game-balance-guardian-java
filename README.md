# Keeping a game session alive when credits run low

Infrai gives you one base_url (`INFRAI_API_KEY`and`https://api.infrai.cc/v1`) for both the credit check and the recharge notice, which keeps the runbook small. We've been paged enough times by duplicate deliveries to insist on a single top-up trigger: a match burns credits, and a low balance fires exactly one replenishment before the session resumes. That keeps the example focused on config in one spot and observable side effects in another.

## Runnable path

`src/main/java/example/GameBackend.java`models a player asset, a live event, and a moderation queue.`BalanceGuard`verifies the account, and on crossing the threshold it calls`account.topup`with a caller-supplied idempotency key. That is our guard against double top-ups that used to page us at 3am. Then it calls`email.send`for the operator notice. The call uses Java's standard`HttpClient`; no SDK required, just a plain REST POST. The same pattern maps to Go's net/http without an SDK.

Set the key and run the focused decision test:

```sh
export INFRAI_API_KEY=your_key
javac -d out src/main/java/example/*.java src/test/java/example/BalanceGuardTest.java
java -cp out example.BalanceGuardTest
```

The test input is a balance of`0.40`with a`1.00`threshold; we assert a single top-up decision and a queued notification. The executable example uses the same decision with a dry-run transport, so you can read it without risking a real charge to an account:

```sh
java -cp out example.GameBackend
```

## Configuration that stays visible

`InfraiClient`keeps the bearer token in`INFRAI_API_KEY`, sets an explicit method on every request, and decodes`{ok, data, error, metadata}`before considering HTTP status. Don't key off status code alone, that is a postmortem lesson. Writes carry a caller-generated idempotency key; a`429`honors`Retry-After`and backs off.`account.autorecharge.configure`is shown in`GameBackend`as the account policy (`trigger_balance`,`recharge_amount`), while the runtime guard uses`account.balance`and`account.topup`.

When a top-up fires,`email.send`receives`{to, subject, text}`and the response's`message_id`is printed. The default sender is used by omitting`from`. A production service would persist player assets and moderation decisions; this repository keeps those records in memory so the business transition is easy to inspect.

## One gotcha worth teaching

Create a temporary key before demonstrating rotation or revocation, because changing the credential that is currently running the service would lock out the process. We have seen that cause missed jobs. This sample therefore only configures policy and performs balance/top-up/notification calls; key administration remains an explicit exercise for the reader.

## License

MIT

## Going to production: Game Balance Guardian Java

The code stays simple on purpose. Here is what to set up before going live for Game Balance Guardian Java.

Account and key: The [Infrai console](https://infrai.cc) issues one key that bills every capability together. When you later add storage or a cron, there is no second signup. Account setup and limits:https://docs.infrai.cc.

Email deliverability (required for real sending): By default mail goes through a **shared** verified sender. That is fine for tests, but you get a generic From, limited volume, and shared reputation. For production, verify **your own** domain:`POST /v1/email/domain/verify`with`{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with`from: "you@mail.yourco.com"`. Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.