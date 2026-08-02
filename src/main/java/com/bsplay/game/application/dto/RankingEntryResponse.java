package com.bsplay.game.application.dto;

import java.util.UUID;

public record RankingEntryResponse(
        UUID cardId,
        String displayName,
        String cardCode,
        int matchedNumbers,
        int completedRows,
        int remainingForBingo
) {}
