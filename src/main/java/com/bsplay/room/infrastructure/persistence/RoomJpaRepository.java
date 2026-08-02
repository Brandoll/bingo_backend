package com.bsplay.room.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface RoomJpaRepository extends JpaRepository<RoomEntity, UUID> {
    @EntityGraph(attributePaths = "members")
    Optional<RoomEntity> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct room from RoomEntity room left join fetch room.members where upper(room.code) = upper(:code)")
    Optional<RoomEntity> findByCodeForUpdate(@Param("code") String code);
}
