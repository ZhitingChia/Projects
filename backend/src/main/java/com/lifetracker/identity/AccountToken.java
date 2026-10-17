package com.lifetracker.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_tokens")
public class AccountToken {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false)
  public UUID userId;

  @Column(nullable = false, length = 20)
  public String purpose;

  @Column(nullable = false, length = 64, unique = true)
  public String tokenHash;

  @Column(nullable = false)
  public Instant expiresAt;

  public Instant consumedAt;
}
