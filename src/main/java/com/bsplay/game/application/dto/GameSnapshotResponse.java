package com.bsplay.game.application.dto;

import com.bsplay.game.domain.model.GameStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameSnapshotResponse(
        UUID id,
        UUID roomId,
        int roundNumber,
        GameStatus status,
        Integer currentNumber,
        Integer previousNumber,
        List<Integer> drawnNumbers,
        int remainingNumbers,
        boolean automaticDrawEnabled,
        int automaticDrawIntervalSeconds,
        boolean lineEnabled,
        boolean doubleLineEnabled,
        boolean bingoEnabled,
        boolean rankingPublic,
        Instant startedAt,
        Instant pausedAt,
        Instant endedAt,
        List<RankingEntryResponse> ranking,
        List<PrizeClaimResponse> claims
) {}
