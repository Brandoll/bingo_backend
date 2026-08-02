package com.bsplay.game.infrastructure.persistence;

import com.bsplay.game.domain.model.ClaimStatus;
import com.bsplay.game.domain.model.PrizeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrizeClaimJpaRepository extends JpaRepository<PrizeClaimEntity, UUID> {
    List<PrizeClaimEntity> findByGameIdOrderByClaimedAtDesc(UUID gameId);
    boolean existsByGameIdAndGameCardIdAndPrizeTypeAndStatus(
            UUID gameId, UUID gameCardId, PrizeType prizeType, ClaimStatus status);
    long countByGameIdAndStatus(UUID gameId, ClaimStatus status);
}
