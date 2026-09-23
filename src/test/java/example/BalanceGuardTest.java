package example;

public final class BalanceGuardTest {
  public static void main(String[] args) {
    BalanceGuard.Decision d = new BalanceGuard().decide(0.40, 1.00, "match-17");
    if (!d.recharge() || !d.idempotencyKey().equals("game-event-match-17")) throw new AssertionError(d);
    System.out.println("PASS: low balance schedules one idempotent recharge");
  }
}
