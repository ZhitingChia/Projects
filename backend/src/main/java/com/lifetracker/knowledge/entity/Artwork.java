package com.lifetracker.knowledge.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "artwork")
public class Artwork {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false)
  public UUID ownerId;

  public UUID libraryEntryId;

  @Column(nullable = false, length = 10)
  public String artworkType;

  @Column(nullable = false, length = 200)
  public String storageKey;

  @Column(nullable = false, length = 30)
  public String mimeType;

  @Column(nullable = false)
  public long byteSize;

  @Column(nullable = false)
  public int width;

  @Column(nullable = false)
  public int height;

  @Column(nullable = false, length = 7)
  public String color;

  public Instant expiresAt;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
