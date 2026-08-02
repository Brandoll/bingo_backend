package com.bsplay.room.application.dto;

import com.bsplay.room.domain.model.RoomStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String code,
        String name,
        RoomStatus status,
        boolean locked,
        int maxPlayers,
        int cardsPerPlayer,
        boolean allowLateJoin,
        boolean hideParticipantNames,
        Instant createdAt,
        List<MemberResponse> members
) {}
