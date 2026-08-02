package com.bsplay.game.application.dto;

import java.util.List;
import java.util.UUID;

public record PhysicalCardResponse(
        UUID id,
        String externalId,
        List<Integer> numbers,
        List<List<Integer>> grid
) {}
