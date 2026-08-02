package com.bsplay.room.presentation.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank @Size(max = 80) String roomName,
        @NotBlank @Size(min = 2, max = 40) String hostName,
        @Min(2) @Max(300) int maxPlayers
) {}
