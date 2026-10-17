package com.lifetracker.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false, unique = true, length = 254)
  public String email;

  @Column(name = "password_hash", nullable = false)
  public String passwordHash;

  public Instant verifiedAt;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
