package com.bsplay.game.domain.service;

import com.bsplay.game.domain.model.PrizeType;

import java.util.List;
import java.util.Set;

public final class PrizeEligibility {
    private PrizeEligibility() {}

    public static boolean isEligible(List<List<Integer>> grid, Set<Integer> drawn, PrizeType type) {
        long completedRows = grid.stream()
                .filter(row -> row.stream().filter(value -> value != null).allMatch(drawn::contains))
                .count();
        return switch (type) {
            case LINE -> completedRows >= 1;
            case DOUBLE_LINE -> completedRows >= 2;
            case BINGO -> grid.stream().flatMap(List::stream)
                    .filter(value -> value != null).allMatch(drawn::contains);
        };
    }
}
