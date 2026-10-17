package com.lifetracker.knowledge.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "books")
public class Book {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false, length = 500)
  public String title;

  @Column(length = 500)
  public String subtitle;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  public List<String> authors = new ArrayList<>();

  @Column(length = 500)
  public String publisher;

  @Column(length = 100)
  public String publicationDate;

  @Column(length = 40)
  public String language;

  public Integer pageCount;

  @Column(length = 2000)
  public String coverUrl;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
