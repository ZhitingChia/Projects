package com.lifetracker.knowledge.provider;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProviderHttp {
  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  private final ObjectMapper json = JsonMapper.builder().build();

  public JsonNode get(String url) {
    try {
      HttpRequest req =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(Duration.ofSeconds(6))
              .header("User-Agent", "LifeTracker/1.0 (personal reading library)")
              .header("Accept", "application/json")
              .header("Cache-Control", "no-cache, no-store")
              .GET()
              .build();
      var response = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
      try (var stream = response.body()) {
        if (response.statusCode() != 200) throw new IllegalStateException("Provider unavailable");
        byte[] bytes = stream.readNBytes(2_000_001);
        if (bytes.length > 2_000_000)
          throw new IllegalStateException("Provider response too large");
        return json.readTree(bytes);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Provider interrupted");
    } catch (Exception e) {
      throw new IllegalStateException("Provider unavailable");
    }
  }

  public static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  public static String text(JsonNode node, String key) {
    JsonNode v = node.path(key);
    return v.isString() ? v.asString() : null;
  }

  public static List<String> strings(JsonNode array) {
    List<String> result = new ArrayList<>();
    if (array.isArray())
      array.forEach(
          n -> {
            if (n.isString()) result.add(n.asString());
          });
    return result;
  }

  public static String descriptionText(JsonNode node) {
    String raw = node.isString() ? node.asString() : text(node, "value");
    if (raw == null || raw.isBlank()) return null;
    String paragraphs = raw.replaceAll("(?i)<(?:br\\s*/?|/p|/div)>", "\n");
    return Jsoup.parse(paragraphs).wholeText().trim();
  }

  public static String safeCover(String raw) {
    if (raw == null) return null;
    try {
      URI uri = URI.create(raw.replaceFirst("^http:", "https:"));
      if (!"https".equals(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1)
        return null;
      if (!Set.of("books.google.com", "books.googleusercontent.com", "covers.openlibrary.org")
          .contains(uri.getHost())) return null;
      return uri.toString();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
