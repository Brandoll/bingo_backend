package com.bsplay.room.application.port;

import com.bsplay.room.application.dto.RoomResponse;

public interface RoomEventPublisher {
    void roomCreated(RoomResponse room);
    void playerJoined(RoomResponse room);
    void gameStarted(RoomResponse room);
    void roomUpdated(String eventType, RoomResponse room);
}
