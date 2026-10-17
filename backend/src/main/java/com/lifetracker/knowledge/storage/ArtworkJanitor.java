package com.lifetracker.knowledge.storage;

import com.lifetracker.knowledge.repository.*;
import com.lifetracker.knowledge.service.ArtworkService;
import java.time.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ArtworkJanitor {
  private final Artworks artworks;
  private final Cleanups cleanups;
  private final ArtworkStorage storage;
  private final ArtworkService service;

  public ArtworkJanitor(
      Artworks artworks, Cleanups cleanups, ArtworkStorage storage, ArtworkService service) {
    this.artworks = artworks;
    this.cleanups = cleanups;
    this.storage = storage;
    this.service = service;
  }

  @Scheduled(fixedDelay = 600000, initialDelay = 60000)
  @Transactional
  public void clean() {
    artworks.findTop100ByExpiresAtBefore(Instant.now()).forEach(service::discard);
    for (var task : cleanups.findTop100ByOrderByCreatedAtAsc()) {
      try {
        storage.delete(task.storageKey);
        cleanups.delete(task);
      } catch (RuntimeException ignored) {
        /* durable task retries next sweep */
      }
    }
    for (String key : storage.olderThan(Instant.now().minus(Duration.ofHours(25))))
      if (!artworks.existsByStorageKey(key)) {
        try {
          storage.delete(key);
        } catch (RuntimeException ignored) {
        }
      }
  }
}
