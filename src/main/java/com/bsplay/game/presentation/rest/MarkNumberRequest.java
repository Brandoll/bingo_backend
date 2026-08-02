package com.bsplay.game.presentation.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MarkNumberRequest(@Min(1) @Max(90) int number) {}
