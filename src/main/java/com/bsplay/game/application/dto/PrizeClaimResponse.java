package com.bsplay.game.application.dto;

import com.bsplay.game.domain.model.ClaimStatus;
import com.bsplay.game.domain.model.PrizeType;

import java.time.Instant;
import java.util.UUID;

public record PrizeClaimResponse(
        UUID id,
        UUID cardId,
        String displayName,
        String cardCode,
        PrizeType prizeType,
        ClaimStatus status,
        Instant claimedAt,
        Instant validatedAt,
        String rejectionReason
) {}
