package com.lifetracker.knowledge.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "book_identifiers")
public class BookIdentifier {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false)
  public UUID bookId;

  @Column(nullable = false, length = 20)
  public String kind;

  @Column(nullable = false, length = 40)
  public String value;
}
