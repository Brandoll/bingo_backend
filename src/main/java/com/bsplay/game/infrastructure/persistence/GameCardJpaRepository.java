package com.bsplay.game.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameCardJpaRepository extends JpaRepository<GameCardEntity, UUID> {
    List<GameCardEntity> findByGameIdAndActiveTrueOrderByAssignedAtAsc(UUID gameId);
    List<GameCardEntity> findByGameIdAndMemberIdAndActiveTrue(UUID gameId, UUID memberId);
    Optional<GameCardEntity> findByIdAndGameId(UUID id, UUID gameId);
    Optional<GameCardEntity> findByGameIdAndPhysicalCardIdAndActiveTrue(UUID gameId, UUID physicalCardId);
    boolean existsByGameIdAndMemberIdAndActiveTrue(UUID gameId, UUID memberId);
    long countByGameId(UUID gameId);
    long countByGameIdAndCardType(UUID gameId, com.bsplay.game.domain.model.CardType cardType);
}
