package com.lifetracker.knowledge.controller;

import com.lifetracker.knowledge.service.*;
import com.lifetracker.shared.RateLimiter;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/artwork")
public class ArtworkController {
  private final ArtworkService artwork;
  private final LibraryService library;
  private final RateLimiter limiter;

  public ArtworkController(ArtworkService artwork, LibraryService library, RateLimiter limiter) {
    this.artwork = artwork;
    this.library = library;
    this.limiter = limiter;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<Map<String, Object>> stage(
      Principal p, @RequestParam String type, @RequestPart MultipartFile file) throws IOException {
    limiter.check("upload:" + p.getName(), 15);
    var a = artwork.stage(library.owner(p.getName()), type, file.getBytes(), file.getContentType());
    return ResponseEntity.status(201)
        .body(
            Map.of(
                "id", a.id, "url", "/api/v1/artwork/" + a.id + "/content", "type", a.artworkType));
  }

  @GetMapping("/{id}/content")
  ResponseEntity<byte[]> content(Principal p, @PathVariable UUID id) {
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .body(artwork.content(library.owner(p.getName()), id));
  }
}
