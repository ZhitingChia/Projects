package com.lifetracker.knowledge.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "library_entries")
public class LibraryEntry {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false)
  public UUID userId;

  @Column(nullable = false)
  public UUID bookId;

  @Column(nullable = false, length = 30)
  public String readingStatus = "COMPLETED";

  @Column(nullable = false)
  public Instant dateAdded = Instant.now();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  public Map<String, Object> metadataOverrides = new LinkedHashMap<>();

  @Column(nullable = false, length = 7)
  public String spineColor;

  @Column(nullable = false)
  public int spineWidth;

  @Column(nullable = false)
  public int spineHeight;

  @Column(nullable = false)
  public int spineStyleVersion = 1;

  @Version public long version;

  @Column(nullable = false)
  public UUID requestId;
}
