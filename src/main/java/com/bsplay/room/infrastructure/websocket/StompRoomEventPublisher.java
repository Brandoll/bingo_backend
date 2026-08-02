package com.bsplay.room.infrastructure.websocket;

import com.bsplay.room.application.dto.RoomResponse;
import com.bsplay.room.application.port.RoomEventPublisher;
import com.bsplay.shared.websocket.RealtimeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
public class StompRoomEventPublisher implements RoomEventPublisher {
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public StompRoomEventPublisher(SimpMessagingTemplate messagingTemplate, Clock clock) {
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Override public void roomCreated(RoomResponse room) { publish("ROOM_CREATED", room); }
    @Override public void playerJoined(RoomResponse room) { publish("PLAYER_JOINED", room); }
    @Override public void gameStarted(RoomResponse room) { publish("GAME_STARTED", room); }
    @Override public void roomUpdated(String eventType, RoomResponse room) { publish(eventType, room); }

    private void publish(String eventType, RoomResponse room) {
        var event = new RealtimeEvent(UUID.randomUUID(), eventType, room.id(),
                clock.instant(), 1, room);
        messagingTemplate.convertAndSend("/topic/rooms/" + room.id(), event);
    }
}
