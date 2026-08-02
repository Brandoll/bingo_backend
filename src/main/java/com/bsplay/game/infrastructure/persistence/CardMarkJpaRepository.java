package com.bsplay.game.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardMarkJpaRepository extends JpaRepository<CardMarkEntity, UUID> {
    List<CardMarkEntity> findByGameCardId(UUID gameCardId);
    Optional<CardMarkEntity> findByGameCardIdAndNumber(UUID gameCardId, int number);
}
