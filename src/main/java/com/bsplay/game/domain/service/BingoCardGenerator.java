package com.bsplay.game.domain.service;

import com.bsplay.game.domain.model.GeneratedCard;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

public final class BingoCardGenerator {
    private final RandomGenerator random;

    public BingoCardGenerator() {
        this(new SecureRandom());
    }

    public BingoCardGenerator(RandomGenerator random) {
        this.random = random;
    }

    public GeneratedCard generate() {
        boolean[][] occupied = generateOccupancy();
        List<List<Integer>> grid = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            grid.add(new ArrayList<>(Collections.nCopies(9, null)));
        }

        for (int column = 0; column < 9; column++) {
            List<Integer> rows = new ArrayList<>();
            for (int row = 0; row < 3; row++) if (occupied[row][column]) rows.add(row);
            List<Integer> values = randomValuesForColumn(column, rows.size());
            for (int index = 0; index < rows.size(); index++) {
                grid.get(rows.get(index)).set(column, values.get(index));
            }
        }

        List<Integer> numbers = grid.stream().flatMap(List::stream)
                .filter(value -> value != null).sorted().toList();
        return new GeneratedCard(grid.stream()
                .map(row -> Collections.unmodifiableList(new ArrayList<>(row))).toList(), numbers);
    }

    private boolean[][] generateOccupancy() {
        for (int attempt = 0; attempt < 10_000; attempt++) {
            boolean[][] occupied = new boolean[3][9];
            for (int row = 0; row < 3; row++) {
                List<Integer> columns = new ArrayList<>();
                for (int column = 0; column < 9; column++) columns.add(column);
                Collections.shuffle(columns, new java.util.Random(random.nextLong()));
                for (int index = 0; index < 5; index++) occupied[row][columns.get(index)] = true;
            }
            boolean allColumnsUsed = true;
            for (int column = 0; column < 9; column++) {
                if (!occupied[0][column] && !occupied[1][column] && !occupied[2][column]) {
                    allColumnsUsed = false;
                    break;
                }
            }
            if (allColumnsUsed) return occupied;
        }
        throw new IllegalStateException("No se pudo generar la estructura de un cartón válido.");
    }

    private List<Integer> randomValuesForColumn(int column, int count) {
        int start = column == 0 ? 1 : column * 10;
        int end = column == 8 ? 90 : column * 10 + 9;
        List<Integer> pool = new ArrayList<>();
        for (int value = start; value <= end; value++) pool.add(value);
        Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        return pool.stream().limit(count).sorted().toList();
    }
}
