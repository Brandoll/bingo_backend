package com.bsplay.room.infrastructure.persistence;

import com.bsplay.room.domain.model.Room;
import com.bsplay.room.domain.repository.RoomRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaRoomRepositoryAdapter implements RoomRepository {
    private final RoomJpaRepository repository;

    public JpaRoomRepositoryAdapter(RoomJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Room save(Room room) {
        RoomEntity entity = repository.findById(room.getId()).orElseGet(() -> RoomEntity.from(room));
        entity.sync(room);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<Room> findByCode(String code) {
        return repository.findByCodeIgnoreCase(code).map(RoomEntity::toDomain);
    }

    @Override
    public Optional<Room> findByCodeForUpdate(String code) {
        return repository.findByCodeForUpdate(code).map(RoomEntity::toDomain);
    }
}
