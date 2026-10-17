package com.lifetracker.knowledge.repository;

import com.lifetracker.knowledge.entity.StorageCleanup;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Cleanups extends JpaRepository<StorageCleanup, UUID> {
  List<StorageCleanup> findTop100ByOrderByCreatedAtAsc();
}
