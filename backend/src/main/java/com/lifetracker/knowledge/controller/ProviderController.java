package com.lifetracker.knowledge.controller;

import com.lifetracker.knowledge.dto.Metadata;
import com.lifetracker.knowledge.provider.*;
import com.lifetracker.shared.RateLimiter;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
public class ProviderController {
  private final ProviderService providers;
  private final RateLimiter limiter;

  public ProviderController(ProviderService providers, RateLimiter limiter) {
    this.providers = providers;
    this.limiter = limiter;
  }

  @GetMapping("/search")
  ResponseEntity<ProviderService.Search> search(
      Principal p,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String author,
      @RequestParam(required = false) String isbn,
      @RequestParam(defaultValue = "0") int page) {
    limiter.check("search:" + p.getName(), 30);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(providers.search(title, author, isbn, page));
  }

  @GetMapping("/providers/{provider}/{id}")
  ResponseEntity<Metadata> details(
      Principal p, @PathVariable String provider, @PathVariable String id) {
    limiter.check("metadata:" + p.getName(), 30);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(providers.details(provider, id));
  }

  @GetMapping("/providers/{provider}/{id}/description")
  ResponseEntity<BookProvider.Description> description(
      Principal p, @PathVariable String provider, @PathVariable String id) {
    limiter.check("description:" + p.getName(), 30);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(providers.description(provider, id));
  }
}
