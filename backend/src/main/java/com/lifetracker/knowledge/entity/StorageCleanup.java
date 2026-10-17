package com.lifetracker.knowledge.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "storage_cleanup")
public class StorageCleanup {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false, length = 200)
  public String storageKey;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
