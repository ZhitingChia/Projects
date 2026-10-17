package com.lifetracker.knowledge.repository;

import com.lifetracker.knowledge.entity.Artwork;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Artworks extends JpaRepository<Artwork, UUID> {
  List<Artwork> findByLibraryEntryIdIn(Collection<UUID> ids);

  Optional<Artwork> findByIdAndOwnerId(UUID id, UUID ownerId);

  List<Artwork> findByLibraryEntryId(UUID entryId);

  Optional<Artwork> findByLibraryEntryIdAndArtworkType(UUID entryId, String type);

  List<Artwork> findTop100ByExpiresAtBefore(Instant now);

  boolean existsByStorageKey(String key);
}
