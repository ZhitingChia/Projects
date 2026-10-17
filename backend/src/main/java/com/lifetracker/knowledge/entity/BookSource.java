package com.lifetracker.knowledge.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "book_sources")
public class BookSource {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false)
  public UUID bookId;

  @Column(nullable = false, length = 30)
  public String provider;

  @Column(nullable = false, length = 100)
  public String editionId;

  @Column(length = 100)
  public String workId;
}
