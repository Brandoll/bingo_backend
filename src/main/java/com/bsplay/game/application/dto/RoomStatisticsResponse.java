package com.bsplay.game.application.dto;

import java.util.List;
import java.util.UUID;

public record RoomStatisticsResponse(
        UUID roomId,
        int totalRounds,
        long totalDraws,
        long totalCards,
        long physicalCards,
        long approvedPrizes,
        List<RoundStatisticsResponse> rounds
) {}
