package com.bsplay.room.domain.repository;

import com.bsplay.room.domain.model.Room;

import java.util.Optional;

public interface RoomRepository {
    Room save(Room room);
    Optional<Room> findByCode(String code);
    Optional<Room> findByCodeForUpdate(String code);
}
