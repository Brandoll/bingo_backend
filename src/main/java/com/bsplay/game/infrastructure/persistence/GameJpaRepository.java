package com.bsplay.game.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameJpaRepository extends JpaRepository<GameEntity, UUID> {
    Optional<GameEntity> findTopByRoomIdOrderByRoundNumberDesc(UUID roomId);
    List<GameEntity> findByRoomIdOrderByRoundNumberDesc(UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GameEntity> findFirstByRoomIdOrderByRoundNumberDesc(UUID roomId);

    @Query("select game.id from GameEntity game where game.status = com.bsplay.game.domain.model.GameStatus.RUNNING " +
            "and game.automaticDrawEnabled = true and game.nextAutomaticDrawAt <= :now")
    List<UUID> findAutomaticGamesDue(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select game from GameEntity game where game.id = :id")
    Optional<GameEntity> findByIdForUpdate(@Param("id") UUID id);
}
