package com.bsplay.game.domain.service;

import com.bsplay.game.domain.model.PrizeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PrizeEligibilityTest {
    private final List<List<Integer>> grid = List.of(
            java.util.Arrays.asList(1, 10, null, 30, null, 50, null, 70, null),
            java.util.Arrays.asList(null, 11, 20, null, 40, null, 60, null, 80),
            java.util.Arrays.asList(2, null, 21, 31, null, 51, null, null, 81));

    @Test
    void validatesLineDoubleLineAndBingo() {
        Set<Integer> firstLine = Set.of(1, 10, 30, 50, 70);
        assertThat(PrizeEligibility.isEligible(grid, firstLine, PrizeType.LINE)).isTrue();
        assertThat(PrizeEligibility.isEligible(grid, firstLine, PrizeType.DOUBLE_LINE)).isFalse();

        Set<Integer> twoLines = Set.of(1, 10, 30, 50, 70, 11, 20, 40, 60, 80);
        assertThat(PrizeEligibility.isEligible(grid, twoLines, PrizeType.DOUBLE_LINE)).isTrue();
        assertThat(PrizeEligibility.isEligible(grid, twoLines, PrizeType.BINGO)).isFalse();

        Set<Integer> bingo = Set.of(1, 10, 30, 50, 70, 11, 20, 40, 60, 80, 2, 21, 31, 51, 81);
        assertThat(PrizeEligibility.isEligible(grid, bingo, PrizeType.BINGO)).isTrue();
    }
}
