package com.lifetracker.knowledge.repository;

import com.lifetracker.knowledge.entity.ReadingSession;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Sessions extends JpaRepository<ReadingSession, UUID> {
  List<ReadingSession> findByLibraryEntryIdInOrderByCompletionDateDescCreatedAtDesc(
      Collection<UUID> ids);

  List<ReadingSession> findByLibraryEntryIdOrderByCompletionDateDescCreatedAtDesc(UUID entryId);

  Optional<ReadingSession> findByIdAndLibraryEntryId(UUID id, UUID entryId);

  Optional<ReadingSession> findByLibraryEntryIdAndRequestId(UUID entryId, UUID requestId);

  long countByLibraryEntryId(UUID entryId);
}
