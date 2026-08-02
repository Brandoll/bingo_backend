package com.bsplay.room.application.dto;

import com.bsplay.room.domain.model.ConnectionStatus;
import com.bsplay.room.domain.model.MemberRole;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        String displayName,
        MemberRole role,
        ConnectionStatus connectionStatus,
        Instant joinedAt
) {}
