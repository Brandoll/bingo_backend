package com.bsplay.game.presentation.rest;

import com.bsplay.game.domain.model.PrizeType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClaimPrizeRequest(@NotNull UUID cardId, @NotNull PrizeType prizeType) {}
