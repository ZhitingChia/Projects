package com.lifetracker.knowledge.controller;

import com.lifetracker.knowledge.dto.*;
import com.lifetracker.knowledge.provider.*;
import com.lifetracker.knowledge.service.LibraryService;
import com.lifetracker.shared.RateLimiter;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/library/books")
public class LibraryController {
  private final LibraryService library;
  private final ProviderService providers;
  private final RateLimiter limiter;

  public LibraryController(LibraryService library, ProviderService providers, RateLimiter limiter) {
    this.library = library;
    this.providers = providers;
    this.limiter = limiter;
  }

  private UUID owner(Principal p) {
    return library.owner(p.getName());
  }

  @GetMapping
  LibraryView.Page list(
      Principal p,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {
    return library.list(owner(p), page, size);
  }

  @PostMapping
  ResponseEntity<LibraryView> create(Principal p, @Valid @RequestBody Requests.Create request) {
    limiter.check("create:" + p.getName(), 30);
    var v = library.create(owner(p), request);
    return ResponseEntity.created(URI.create("/api/v1/library/books/" + v.id())).body(v);
  }

  @GetMapping("/{id}")
  LibraryView get(Principal p, @PathVariable UUID id) {
    return library.get(owner(p), id);
  }

  @PatchMapping("/{id}")
  LibraryView edit(
      Principal p, @PathVariable UUID id, @Valid @RequestBody Requests.EditMetadata r) {
    return library.edit(owner(p), id, r);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(Principal p, @PathVariable UUID id) {
    library.delete(owner(p), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/sessions")
  ResponseEntity<LibraryView> add(
      Principal p, @PathVariable UUID id, @Valid @RequestBody Requests.Reading r) {
    return ResponseEntity.status(201).body(library.addSession(owner(p), id, r));
  }

  @PatchMapping("/{id}/sessions/{sessionId}")
  LibraryView editSession(
      Principal p,
      @PathVariable UUID id,
      @PathVariable UUID sessionId,
      @Valid @RequestBody Requests.EditReading r) {
    return library.editSession(owner(p), id, sessionId, r);
  }

  @DeleteMapping("/{id}/sessions/{sessionId}")
  ResponseEntity<Void> deleteSession(
      Principal p, @PathVariable UUID id, @PathVariable UUID sessionId) {
    library.deleteSession(owner(p), id, sessionId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}/artwork/{type}")
  LibraryView attach(
      Principal p,
      @PathVariable UUID id,
      @PathVariable String type,
      @Valid @RequestBody Requests.Attach r) {
    return library.attach(owner(p), id, type, r.artworkId());
  }

  @DeleteMapping("/{id}/artwork/{type}")
  LibraryView remove(Principal p, @PathVariable UUID id, @PathVariable String type) {
    return library.removeArtwork(owner(p), id, type);
  }

  @GetMapping("/{id}/description")
  ResponseEntity<BookProvider.Description> description(Principal p, @PathVariable UUID id) {
    var m = library.get(owner(p), id).metadata();
    limiter.check("description:" + p.getName(), 30);
    var d =
        m.provider() == null
            ? new BookProvider.Description(null, "No external source", false)
            : providers.description(m.provider(), m.editionId());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(d);
  }
}
