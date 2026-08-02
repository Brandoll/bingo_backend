package com.bsplay.game.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawnNumberJpaRepository extends JpaRepository<DrawnNumberEntity, UUID> {
    List<DrawnNumberEntity> findByGameIdOrderByDrawOrderAsc(UUID gameId);
    Optional<DrawnNumberEntity> findTopByGameIdOrderByDrawOrderDesc(UUID gameId);
    boolean existsByGameIdAndNumber(UUID gameId, int number);
    long countByGameId(UUID gameId);
}
