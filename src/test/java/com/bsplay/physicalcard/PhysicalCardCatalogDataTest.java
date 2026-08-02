package com.bsplay.physicalcard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PhysicalCardCatalogDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void containsSeventyTwoUniqueValidNinetyBallCards() throws Exception {
        JsonNode cards = objectMapper.readTree(getClass().getResourceAsStream("/data/physical-cards.json"));
        Set<String> ids = new HashSet<>();
        Set<String> signatures = new HashSet<>();

        assertThat(cards.size()).isEqualTo(72);
        for (JsonNode card : cards) {
            String id = card.path("externalId").asText();
            List<Integer> sourceNumbers = new ArrayList<>();
            card.path("numbers").forEach(number -> sourceNumbers.add(number.asInt()));
            JsonNode grid = card.path("grid");

            assertThat(ids.add(id)).isTrue();
            assertThat(sourceNumbers).hasSize(15).doesNotHaveDuplicates()
                    .allMatch(number -> number >= 1 && number <= 90);
            assertThat(signatures.add(sourceNumbers.toString())).isTrue();
            assertThat(grid.size()).isEqualTo(3);

            List<Integer> gridNumbers = new ArrayList<>();
            for (JsonNode row : grid) {
                assertThat(row.size()).isEqualTo(9);
                int filled = 0;
                for (int column = 0; column < 9; column++) {
                    JsonNode value = row.get(column);
                    if (!value.isNull()) {
                        int number = value.asInt();
                        int minimum = column == 0 ? 1 : column * 10;
                        int maximum = column == 8 ? 90 : column * 10 + 9;
                        assertThat(number).isBetween(minimum, maximum);
                        gridNumbers.add(number);
                        filled++;
                    }
                }
                assertThat(filled).isEqualTo(5);
            }
            assertThat(gridNumbers).containsExactlyInAnyOrderElementsOf(sourceNumbers);
        }
    }
}
