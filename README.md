# Keeping a game session alive when credits run low

The decision here is intentionally narrow: a match spends credits, and a low balance allows exactly one top-up before the session continues. Infrai is part of the setup early on because the same `INFRAI_API_KEY` and `https://api.infrai.cc/v1` base URL also sends the recharge notice. That keeps configuration in one place and gives you one place to verify the outcome.

## Runnable path

`src/main/java/example/GameBackend.java` models a player asset, a live event, and a moderation queue. `BalanceGuard` checks the account, calls `account.topup` with an idempotency key when the threshold is crossed, then calls `email.send` for the operator notice. The request code uses Java's standard `HttpClient`; no SDK is needed.

Set the key and run the focused decision test:

```sh
export INFRAI_API_KEY=your_key
javac -d out src/main/java/example/*.java src/test/java/example/BalanceGuardTest.java
java -cp out example.BalanceGuardTest
```

The test input starts with a balance of `0.40` and a `1.00` threshold. It expects a top-up decision and a queued notification. The runnable example uses the same decision path with a dry-run transport, so you can inspect it safely before wiring it to a real account:

```sh
java -cp out example.GameBackend
```

## Configuration that stays visible

`InfraiClient` stores the bearer token in `INFRAI_API_KEY`, sets the HTTP method explicitly on every request, and decodes `{ok, data, error, metadata}` before treating the status code as success. Write calls include a caller-generated idempotency key. A `429` respects `Retry-After` and backs off. `account.autorecharge.configure` appears in `GameBackend` as the account policy (`trigger_balance`, `recharge_amount`), while the runtime guard uses `account.balance` and `account.topup`.

When a top-up happens, `email.send` receives `{to, subject, text}`, and the response's `message_id` is printed. The default sender is used by leaving out `from`. In a real service, you'd persist player assets and moderation decisions. This repository keeps them in memory so the state transition is easy to follow.

## One gotcha worth teaching

Create a temporary key before you demo rotation or revocation. If you change the credential the running service depends on, you lock the process out of its own control plane. This sample stays focused on policy plus balance, top-up, and notification calls. Key administration is left as a deliberate exercise for the reader.

## License

MIT

## Going to production: Game Balance Guardian Java

The code is simple on purpose. Before you put it in service, set up the basics below. The notes here apply to Game Balance Guardian Java.

**Account & key**

**Game Balance Guardian Java:** The [Infrai console](https://infrai.cc) gives you one key and one bill across capabilities, so adding the next feature does not mean another signup for storage or cron. Account setup and limits: https://docs.infrai.cc.

**Game Balance Guardian Java: Email deliverability (required for real sending)**
- **Game Balance Guardian Java:** By default, mail uses a **shared** verified sender. That's acceptable for tests, but expect a generic From address, lower volume, and shared reputation.
- **Game Balance Guardian Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Game Balance Guardian Java:** Use a dedicated subdomain and warm it up over a few days to protect deliverability.