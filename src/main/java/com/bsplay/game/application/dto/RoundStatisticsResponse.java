package com.bsplay.game.application.dto;

import com.bsplay.game.domain.model.GameStatus;

import java.time.Instant;
import java.util.UUID;

public record RoundStatisticsResponse(
        UUID gameId,
        int roundNumber,
        GameStatus status,
        int drawnNumbers,
        long assignedCards,
        long physicalCards,
        long approvedPrizes,
        Instant startedAt,
        Instant endedAt
) {}
