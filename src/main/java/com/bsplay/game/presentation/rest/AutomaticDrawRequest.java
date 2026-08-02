package com.bsplay.game.presentation.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AutomaticDrawRequest(
        boolean enabled,
        @Min(3) @Max(60) int intervalSeconds
) {}
