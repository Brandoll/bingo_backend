package com.bsplay.game.infrastructure.websocket;

import com.bsplay.game.application.dto.GameSnapshotResponse;
import com.bsplay.game.application.port.GameEventPublisher;
import com.bsplay.shared.websocket.RealtimeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
public class StompGameEventPublisher implements GameEventPublisher {
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public StompGameEventPublisher(SimpMessagingTemplate messagingTemplate, Clock clock) {
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Override
    public void publish(String eventType, GameSnapshotResponse game) {
        var event = new RealtimeEvent(UUID.randomUUID(), eventType, game.roomId(),
                clock.instant(), 1, game);
        messagingTemplate.convertAndSend("/topic/games/" + game.id(), event);
        messagingTemplate.convertAndSend("/topic/rooms/" + game.roomId() + "/game", event);
    }
}
