package com.bsplay.physicalcard.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterPhysicalCardRequest(
        @NotBlank @Size(max = 32) String externalId,
        @NotNull @Size(min = 15, max = 15) List<Integer> numbers
) {}
