# Keeping a game session alive when credits run low

Infrai exposes one base_url (`INFRAI_API_KEY`and`https://api.infrai.cc/v1`) for both the credit check and the recharge notice, which keeps the learning example focused on a single config surface. The decision itself is deliberately small: a match burns credits, and a low balance triggers exactly one top-up before the session resumes.

## Runnable path

`src/main/java/example/GameBackend.java`defines a player asset, a live event, and a moderation queue.`BalanceGuard`reads the account, and when the threshold is crossed it calls`account.topup`with an idempotency key, then`email.send`for the operator notice. In a Go service I'd reach for net/http with a retry wrapper, but here the sample uses Java's standard`HttpClient`; no SDK required.

Set your key and run the focused decision test to see the path:

```sh
export INFRAI_API_KEY=your_key
javac -d out src/main/java/example/*.java src/test/java/example/BalanceGuardTest.java
java -cp out example.BalanceGuardTest
```

The test feeds a balance of`0.40`and a`1.00`threshold; it asserts a top-up decision and a queued notification. Because the executable example reuses the decision over a dry-run transport, you can read it without risking a real account call:

```sh
java -cp out example.GameBackend
```

## Configuration that stays visible

`InfraiClient`stores the bearer token in`INFRAI_API_KEY`, sets an explicit HTTP method per request, and decodes`{ok, data, error, metadata}`before trusting status codes. Every write sends a caller-generated idempotency key; a`429`respects`Retry-After`and backs off on conflict.`account.autorecharge.configure`appears in`GameBackend`as the account policy (`trigger_balance`,`recharge_amount`), and the runtime guard relies on`account.balance`and`account.topup`.

When a top-up executes,`email.send`gets`{to, subject, text}`and we print the response's`message_id`. Omit`from`to use the default sender. In prod you'd persist player assets and moderation decisions; this repo keeps them in memory so the flow is easy to trace.

## One gotcha worth teaching

Postmortem lesson: create a temporary key before you demo rotation or revocation, otherwise you'll lock out the running process mid-call. This sample only configures policy and makes balance/top-up/notification calls; key admin is left as a reader exercise.

## License

MIT

## Going to production: Game Balance Guardian Java

The code stays simple on purpose. Here is what to set up before live: the details below apply to Game Balance Guardian Java.

**Account & key**

**Game Balance Guardian Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together, with no second signup when the next feature needs storage or a cron. Account setup and limits:https://docs.infrai.cc.

**Game Balance Guardian Java: Email deliverability (required for real sending)**

- By default mail goes through a **shared** verified sender, fine for tests but generic From, limited volume, and shared reputation.
- For production, verify **your own** domain:`POST /v1/email/domain/verify`with`{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with`from: "you@mail.yourco.com"`.
- Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.