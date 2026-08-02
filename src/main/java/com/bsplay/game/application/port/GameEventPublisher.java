package com.bsplay.game.application.port;

import com.bsplay.game.application.dto.GameSnapshotResponse;

public interface GameEventPublisher {
    void publish(String eventType, GameSnapshotResponse game);
}
