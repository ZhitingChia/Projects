package com.lifetracker.knowledge.provider.openlibrary;

import static com.lifetracker.knowledge.provider.ProviderHttp.*;

import com.lifetracker.knowledge.dto.Metadata;
import com.lifetracker.knowledge.provider.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class OpenLibraryProvider implements BookProvider {
  private final ProviderHttp http;
  private final String base;

  public OpenLibraryProvider(
      ProviderHttp http, @Value("${app.providers.openlibrary-url}") String base) {
    this.http = http;
    this.base = base;
  }

  public String name() {
    return "openlibrary";
  }

  public List<Metadata> search(String title, String author, String isbn, int page) {
    if (isbn != null) {
      if (page > 0) return List.of();
      JsonNode match = http.get(base + "/isbn/" + encode(isbn) + ".json");
      String key = text(match, "key");
      if (key == null || !key.matches("/books/OL[0-9]+M")) return List.of();
      return List.of(normalizeEdition(key.substring(7), match));
    }
    StringBuilder q =
        new StringBuilder(
            base
                + "/search.json?limit=10&page="
                + (page + 1)
                + "&fields=key,title,author_name,first_publish_year,edition_key,cover_i,isbn");
    if (title != null && !title.isBlank()) q.append("&title=").append(encode(title));
    if (author != null && !author.isBlank()) q.append("&author=").append(encode(author));
    if (isbn != null) q.append("&isbn=").append(encode(isbn));
    List<Metadata> result = new ArrayList<>();
    for (JsonNode doc : http.get(q.toString()).path("docs")) {
      String edition = doc.path("edition_key").path(0).asString("");
      if (!edition.matches("OL[0-9]+M")) continue;
      String work = text(doc, "key");
      result.add(
          new Metadata(
              text(doc, "title"),
              null,
              strings(doc.path("author_name")),
              null,
              null,
              null,
              doc.has("first_publish_year") ? doc.path("first_publish_year").asString() : null,
              null,
              null,
              doc.path("cover_i").asLong(0) > 0
                  ? "https://covers.openlibrary.org/b/id/" + doc.path("cover_i").asLong() + "-M.jpg"
                  : null,
              name(),
              edition,
              work == null ? null : work.replace("/works/", "")));
    }
    return result;
  }

  private JsonNode edition(String id) {
    return http.get(base + "/books/" + encode(id) + ".json");
  }

  public Metadata details(String id) {
    return normalizeEdition(id, edition(id));
  }

  private Metadata normalizeEdition(String id, JsonNode n) {
    List<String> authors = new ArrayList<>();
    // A small bounded number of author lookups; missing author metadata remains optional.
    for (JsonNode a : n.path("authors")) {
      if (authors.size() == 3) break;
      String key = text(a, "key");
      if (key != null && key.matches("/authors/OL[0-9]+A")) {
        try {
          String name = text(http.get(base + key + ".json"), "name");
          if (name != null) authors.add(name);
        } catch (IllegalStateException ignored) {
        }
      }
    }
    String work = text(n.path("works").path(0), "key");
    long cover = n.path("covers").path(0).asLong(0);
    return new Metadata(
        text(n, "title"),
        text(n, "subtitle"),
        authors,
        n.path("isbn_10").path(0).asString(null),
        n.path("isbn_13").path(0).asString(null),
        n.path("publishers").path(0).asString(null),
        text(n, "publish_date"),
        languageCode(n),
        n.path("number_of_pages").asInt(0) > 0 ? n.path("number_of_pages").asInt() : null,
        cover > 0 ? "https://covers.openlibrary.org/b/id/" + cover + "-M.jpg" : null,
        name(),
        id,
        work == null ? null : work.replace("/works/", ""));
  }

  private String languageCode(JsonNode n) {
    String key = text(n.path("languages").path(0), "key");
    return key == null ? null : key.replace("/languages/", "");
  }

  public Description description(String id) {
    JsonNode n = edition(id);
    String description = descriptionText(n.path("description"));
    if (description != null) return new Description(description, "Open Library edition", false);
    String work = text(n.path("works").path(0), "key");
    if (work != null && work.matches("/works/OL[0-9]+W"))
      return new Description(
          descriptionText(http.get(base + work + ".json").path("description")),
          "Open Library work",
          true);
    return new Description(null, "Open Library", false);
  }
}
