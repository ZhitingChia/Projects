package com.lifetracker.knowledge.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "reading_sessions")
public class ReadingSession {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false)
  public UUID libraryEntryId;

  public LocalDate startDate;

  @Column(nullable = false)
  public LocalDate completionDate;

  @Column(length = 20000)
  public String review;

  @Version public long version;

  @Column(nullable = false)
  public UUID requestId;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
