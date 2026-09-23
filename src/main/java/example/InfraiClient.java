package example;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public final class InfraiClient {
  private final HttpClient http = HttpClient.newHttpClient();
  private final String key;
  private final String baseUrl;

  public InfraiClient(String key, String baseUrl) { this.key = key; this.baseUrl = baseUrl; }

  public String request(String method, String path, String body) throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
        .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
        .header("Content-Type", "application/json");
    b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
    int attempt = 0;
    while (true) {
      HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
      String envelope = r.body();
      if (envelope.contains("\"ok\":false")) throw new IllegalStateException("Infrai error: " + envelope);
      if (r.statusCode() == 429 && attempt++ < 3) { Thread.sleep(500L * (1L << (attempt - 1))); continue; }
      if (r.statusCode() >= 500) throw new IllegalStateException("transport status " + r.statusCode());
      return envelope;
    }
  }
}
