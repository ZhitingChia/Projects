package com.lifetracker.knowledge.repository;

import com.lifetracker.knowledge.entity.LibraryEntry;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface Entries extends JpaRepository<LibraryEntry, UUID> {
  @Query(
      value =
          """
          select exists(select 1 from library_entries e join books b on b.id=e.book_id
          where e.user_id=:owner and lower(coalesce(e.metadata_overrides->>'title',b.title))=lower(:title))
          """,
      nativeQuery = true)
  boolean existsPossibleDuplicate(UUID owner, String title);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from LibraryEntry e where e.id = :id and e.userId = :owner")
  Optional<LibraryEntry> lockOwned(UUID id, UUID owner);

  Optional<LibraryEntry> findByIdAndUserId(UUID id, UUID userId);

  Optional<LibraryEntry> findByUserIdAndBookId(UUID userId, UUID bookId);

  Optional<LibraryEntry> findByUserIdAndRequestId(UUID userId, UUID requestId);

  Page<LibraryEntry> findByUserIdAndReadingStatusOrderByDateAddedDescIdDesc(
      UUID userId, String status, Pageable page);
}
