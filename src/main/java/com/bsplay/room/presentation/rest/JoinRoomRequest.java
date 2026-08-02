package com.bsplay.room.presentation.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(@NotBlank @Size(min = 2, max = 40) String displayName) {}
