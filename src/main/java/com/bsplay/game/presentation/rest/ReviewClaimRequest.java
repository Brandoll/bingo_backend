package com.bsplay.game.presentation.rest;

import jakarta.validation.constraints.Size;

public record ReviewClaimRequest(boolean approved, @Size(max = 240) String reason) {}
