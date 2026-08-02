package com.bsplay.game.application.dto;

import com.bsplay.game.domain.model.CardType;

import java.util.List;
import java.util.UUID;

public record GameCardResponse(
        UUID id,
        CardType cardType,
        String displayName,
        String externalCode,
        List<List<Integer>> grid,
        List<Integer> markedNumbers,
        int matchedNumbers,
        int completedRows,
        boolean active
) {}
