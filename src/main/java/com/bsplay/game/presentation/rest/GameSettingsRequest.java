package com.bsplay.game.presentation.rest;

public record GameSettingsRequest(
        boolean lineEnabled,
        boolean doubleLineEnabled,
        boolean bingoEnabled,
        boolean rankingPublic
) {}
