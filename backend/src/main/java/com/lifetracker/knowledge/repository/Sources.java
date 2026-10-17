package com.lifetracker.knowledge.repository;

import com.lifetracker.knowledge.entity.BookSource;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Sources extends JpaRepository<BookSource, UUID> {
  List<BookSource> findByBookIdIn(Collection<UUID> ids);

  Optional<BookSource> findByProviderAndEditionId(String provider, String editionId);

  List<BookSource> findByBookId(UUID bookId);
}
