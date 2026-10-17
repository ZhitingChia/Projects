package com.lifetracker.identity;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TokenRepository extends JpaRepository<AccountToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<AccountToken> findByTokenHash(String tokenHash);

  void deleteByUserIdAndPurpose(UUID userId, String purpose);

  @Modifying
  @Query(
      "update AccountToken t set t.consumedAt = CURRENT_TIMESTAMP where t.userId = :userId and"
          + " t.purpose = :purpose and t.consumedAt is null")
  void invalidateAll(UUID userId, String purpose);
}
