package com.bsplay.room.presentation.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RoomSettingsRequest(
        @Min(1) @Max(4) int cardsPerPlayer,
        boolean allowLateJoin,
        boolean hideParticipantNames
) {}
