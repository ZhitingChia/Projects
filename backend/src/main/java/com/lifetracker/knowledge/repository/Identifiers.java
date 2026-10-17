package com.lifetracker.knowledge.repository;

import com.lifetracker.knowledge.entity.BookIdentifier;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Identifiers extends JpaRepository<BookIdentifier, UUID> {
  List<BookIdentifier> findByBookIdIn(Collection<UUID> ids);

  Optional<BookIdentifier> findByKindAndValue(String kind, String value);

  List<BookIdentifier> findByBookId(UUID bookId);
}
