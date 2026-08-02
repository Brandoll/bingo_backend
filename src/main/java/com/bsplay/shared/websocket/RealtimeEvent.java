package com.bsplay.shared.websocket;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEvent(
        UUID eventId,
        String eventType,
        UUID roomId,
        Instant occurredAt,
        int version,
        Object payload
) {}
