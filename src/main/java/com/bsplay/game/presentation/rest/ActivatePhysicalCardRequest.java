package com.bsplay.game.presentation.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivatePhysicalCardRequest(
        @NotBlank @Size(max = 32) String externalId,
        @NotBlank @Size(min = 2, max = 40) String displayName
) {}
