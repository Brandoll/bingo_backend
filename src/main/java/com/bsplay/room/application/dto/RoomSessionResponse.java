package com.bsplay.room.application.dto;

import com.bsplay.room.domain.model.MemberRole;

import java.util.UUID;

public record RoomSessionResponse(
        String token,
        UUID memberId,
        MemberRole role,
        RoomResponse room
) {}
