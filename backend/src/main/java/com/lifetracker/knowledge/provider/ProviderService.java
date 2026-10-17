package com.lifetracker.knowledge.provider;

import com.lifetracker.knowledge.dto.Metadata;
import com.lifetracker.knowledge.validation.Isbn;
import com.lifetracker.shared.ApiException;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProviderService {
  private final List<BookProvider> providers;

  public ProviderService(List<BookProvider> providers) {
    this.providers = providers;
  }

  public record Search(List<Metadata> items, List<String> warnings) {}

  public Search search(String title, String author, String isbn, int page) {
    if ((title == null || title.isBlank())
        && (author == null || author.isBlank())
        && (isbn == null || isbn.isBlank()))
      throw ApiException.bad("Enter a title, author or ISBN");
    if ((title != null && title.length() > 300)
        || (author != null && author.length() > 300)
        || page < 0
        || page > 20) throw ApiException.bad("Search is too long or page is out of range");
    String normalized = Isbn.normalize(isbn);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<Search>> jobs =
          providers.stream()
              .map(
                  p ->
                      executor.submit(
                          () -> {
                            try {
                              return new Search(
                                  p.search(title, author, normalized, page), List.of());
                            } catch (RuntimeException e) {
                              return new Search(
                                  List.of(),
                                  List.of(
                                      p.name()
                                          + " is unavailable. You can still enter a book"
                                          + " manually."));
                            }
                          }))
              .toList();
      List<Metadata> items = new ArrayList<>();
      List<String> warnings = new ArrayList<>();
      Set<String> seen = new HashSet<>();
      for (var job : jobs)
        try {
          Search s = job.get(25, TimeUnit.SECONDS);
          warnings.addAll(s.warnings);
          for (Metadata m : s.items) {
            String identity = m.isbn13() != null ? m.isbn13() : m.provider() + ":" + m.editionId();
            if (seen.add(identity)) items.add(m);
          }
        } catch (Exception e) {
          warnings.add("A provider timed out. Manual entry remains available.");
        }
      return new Search(items, warnings);
    }
  }

  private BookProvider provider(String name, String id) {
    if (id == null || !id.matches("[A-Za-z0-9_-]{1,100}"))
      throw ApiException.bad("Invalid provider identifier");
    if (name.equals("openlibrary") && !id.matches("OL[0-9]+M"))
      throw ApiException.bad("Select an Open Library edition");
    return providers.stream()
        .filter(p -> p.name().equals(name))
        .findFirst()
        .orElseThrow(() -> ApiException.bad("Unsupported provider"));
  }

  public Metadata details(String name, String id) {
    BookProvider p = provider(name, id);
    try {
      return p.details(id);
    } catch (RuntimeException e) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Book metadata is unavailable. Retry or use manual entry.");
    }
  }

  public BookProvider.Description description(String name, String id) {
    BookProvider p = provider(name, id);
    try {
      return p.description(id);
    } catch (RuntimeException e) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE, "Description is temporarily unavailable");
    }
  }
}
