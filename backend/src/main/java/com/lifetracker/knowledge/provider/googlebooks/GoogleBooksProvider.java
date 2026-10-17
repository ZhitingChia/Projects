package com.lifetracker.knowledge.provider.googlebooks;

import static com.lifetracker.knowledge.provider.ProviderHttp.*;

import com.lifetracker.knowledge.dto.Metadata;
import com.lifetracker.knowledge.provider.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class GoogleBooksProvider implements BookProvider {
  private final ProviderHttp http;
  private final String base, key;

  public GoogleBooksProvider(
      ProviderHttp http,
      @Value("${app.providers.google-url}") String base,
      @Value("${app.providers.google-key}") String key) {
    this.http = http;
    this.base = base;
    this.key = key;
  }

  public String name() {
    return "googlebooks";
  }

  private String credentials() {
    return key.isBlank() ? "" : "&key=" + encode(key);
  }

  public List<Metadata> search(String title, String author, String isbn, int page) {
    List<String> q = new ArrayList<>();
    if (title != null && !title.isBlank()) q.add("intitle:" + title);
    if (author != null && !author.isBlank()) q.add("inauthor:" + author);
    if (isbn != null) q.add("isbn:" + isbn);
    JsonNode root =
        http.get(
            base
                + "/volumes?q="
                + encode(String.join(" ", q))
                + "&maxResults=10&startIndex="
                + (page * 10)
                + credentials());
    List<Metadata> out = new ArrayList<>();
    root.path("items")
        .forEach(
            n -> {
              Metadata m = normalize(n);
              if (m.title() != null) out.add(m);
            });
    return out;
  }

  private JsonNode volume(String id) {
    return http.get(base + "/volumes/" + encode(id) + "?projection=full" + credentials());
  }

  public Metadata details(String id) {
    return normalize(volume(id));
  }

  public Description description(String id) {
    return new Description(
        descriptionText(volume(id).path("volumeInfo").path("description")), "Google Books", false);
  }

  Metadata normalize(JsonNode node) {
    JsonNode v = node.path("volumeInfo");
    String i10 = null, i13 = null;
    for (JsonNode n : v.path("industryIdentifiers")) {
      if ("ISBN_10".equals(text(n, "type"))) i10 = text(n, "identifier");
      if ("ISBN_13".equals(text(n, "type"))) i13 = text(n, "identifier");
    }
    return new Metadata(
        text(v, "title"),
        text(v, "subtitle"),
        strings(v.path("authors")),
        i10,
        i13,
        text(v, "publisher"),
        text(v, "publishedDate"),
        text(v, "language"),
        v.path("pageCount").asInt(0) > 0 ? v.path("pageCount").asInt() : null,
        safeCover(text(v.path("imageLinks"), "thumbnail")),
        name(),
        text(node, "id"),
        null);
  }
}
