package com.bsplay.game.domain.service;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class BingoCardGeneratorTest {
    @Test
    void generatesValidNinetyBallCards() {
        var generator = new BingoCardGenerator(new Random(42));
        for (int sample = 0; sample < 100; sample++) {
            var card = generator.generate();
            assertThat(card.grid()).hasSize(3).allSatisfy(row -> {
                assertThat(row).hasSize(9);
                assertThat(row.stream().filter(Objects::nonNull)).hasSize(5);
            });
            assertThat(card.numbers()).hasSize(15).doesNotHaveDuplicates()
                    .allMatch(number -> number >= 1 && number <= 90);
            for (int column = 0; column < 9; column++) {
                final int currentColumn = column;
                var values = card.grid().stream().map(row -> row.get(currentColumn)).filter(Objects::nonNull).toList();
                assertThat(values).isSorted();
                int start = column == 0 ? 1 : column * 10;
                int end = column == 8 ? 90 : column * 10 + 9;
                assertThat(values).allMatch(value -> value >= start && value <= end);
            }
        }
    }
}
