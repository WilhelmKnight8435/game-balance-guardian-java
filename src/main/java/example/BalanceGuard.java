package example;

public final class BalanceGuard {
  public record Decision(boolean recharge, String idempotencyKey) {}
  public Decision decide(double balance, double trigger, String eventId) {
    return new Decision(balance < trigger, "game-event-" + eventId);
  }
}
