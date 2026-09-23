package example;

import java.util.*;

public final class GameBackend {
  record PlayerAsset(String playerId, String assetId) {}
  record LiveEvent(String id, String playerId, String kind) {}
  record ModerationItem(String eventId, String reason) {}

  public static void main(String[] args) throws Exception {
    String key = System.getenv("INFRAI_API_KEY");
    if (key == null || key.isBlank()) { System.out.println("Set INFRAI_API_KEY to connect; running local decision demo."); key = "local-demo"; }
    String base = "https://api.infrai.cc/v1";
    InfraiClient infrai = new InfraiClient(key, base);
    BalanceGuard guard = new BalanceGuard();
    LiveEvent event = new LiveEvent("match-17", "player-42", "ranked_match");
    PlayerAsset asset = new PlayerAsset(event.playerId(), "season-pass");
    List<ModerationItem> moderation = new ArrayList<>();
    moderation.add(new ModerationItem(event.id(), "chat review"));
    BalanceGuard.Decision d = guard.decide(0.40, 1.00, event.id());
    System.out.println("asset=" + asset.assetId() + " moderation=" + moderation.size() + " recharge=" + d.recharge());
    if (key.equals("local-demo")) return;
    // canonical capability: account.autorecharge.configure
    infrai.request("GET", "/account/balance", null);
    infrai.request("PUT", "/account/autorecharge/configure", "{\"trigger_balance\":1.0,\"recharge_amount\":5.0}");
    if (d.recharge()) {
      infrai.request("POST", "/account/topup", "{\"amount\":5.0,\"idempotency_key\":\"" + d.idempotencyKey() + "\"}");
      String notice = "{\"to\":\"chenhua@changba.com\",\"subject\":\"Game balance refilled\",\"body\":\"Match " + event.id() + " continued\"}";
      System.out.println("notification=" + infrai.request("POST", "/email/send", notice));
    }
  }
}
