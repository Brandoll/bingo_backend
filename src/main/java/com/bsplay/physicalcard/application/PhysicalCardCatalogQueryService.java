package com.bsplay.physicalcard.application;

import com.bsplay.game.application.dto.PhysicalCardResponse;
import com.bsplay.game.domain.exception.GameDomainException;
import com.bsplay.room.domain.model.MemberRole;
import com.bsplay.shared.security.GuestPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PhysicalCardCatalogQueryService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public PhysicalCardCatalogQueryService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PhysicalCardImportReport latestReport() {
        return jdbc.query("""
                select id, file_name, file_checksum, import_version, total_rows, valid_rows,
                       invalid_rows, status, created_at, completed_at
                from physical_card_imports order by created_at desc limit 1
                """, (result, row) -> new PhysicalCardImportReport(
                result.getObject("id", UUID.class), result.getString("file_name"),
                result.getString("file_checksum"), result.getString("import_version"),
                result.getInt("total_rows"), result.getInt("valid_rows"), result.getInt("invalid_rows"),
                result.getString("status"), result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("completed_at") == null ? null : result.getTimestamp("completed_at").toInstant()
        )).stream().findFirst().orElseThrow(() -> new GameDomainException(
                "IMPORT_REPORT_NOT_FOUND", "Todavía no existe un reporte de importación."));
    }

    @Transactional
    public PhysicalCardResponse register(String externalId, List<Integer> submittedNumbers,
                                         GuestPrincipal principal) {
        requirePrimaryHost(principal);
        String normalizedId = externalId == null ? "" : externalId.trim().toUpperCase(Locale.ROOT);
        if (!normalizedId.matches("[A-Z0-9-]{1,32}")) {
            throw new GameDomainException("INVALID_PHYSICAL_CARD_ID", "El ID sólo admite letras, números y guiones.");
        }
        List<Integer> rawNumbers = submittedNumbers == null ? List.of() : submittedNumbers;
        if (rawNumbers.stream().anyMatch(java.util.Objects::isNull)) {
            throw new GameDomainException("INVALID_PHYSICAL_CARD_NUMBERS",
                    "El cartón debe contener 15 números únicos entre 1 y 90.");
        }
        List<Integer> numbers = rawNumbers.stream().sorted().toList();
        List<List<Integer>> grid = buildGrid(numbers);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        try {
            String numbersJson = objectMapper.writeValueAsString(numbers);
            String gridJson = objectMapper.writeValueAsString(grid);
            String structureHash = sha256(numbersJson);
            jdbc.update("""
                    insert into physical_cards
                    (id, external_id, numbers_json, grid_json, structure_hash, source, source_reference,
                     layout_source, created_at, updated_at)
                    values (?, ?, cast(? as jsonb), cast(? as jsonb), ?, 'MANUAL', 'host-panel',
                            'GENERATED_FROM_NUMBERS', ?, ?)
                    """, id, normalizedId, numbersJson, gridJson, structureHash,
                    Timestamp.from(now), Timestamp.from(now));
        } catch (DataIntegrityViolationException exception) {
            throw new GameDomainException("PHYSICAL_CARD_DUPLICATE",
                    "Ya existe un cartón con ese ID o con los mismos números.");
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo registrar el cartón físico.", exception);
        }
        return new PhysicalCardResponse(id, normalizedId, numbers, grid);
    }

    private void requirePrimaryHost(GuestPrincipal principal) {
        if (principal == null || principal.role() != MemberRole.HOST) {
            throw new GameDomainException("HOST_REQUIRED", "Sólo el anfitrión principal puede registrar cartones.");
        }
    }

    private List<List<Integer>> buildGrid(List<Integer> numbers) {
        if (numbers.size() != 15 || numbers.stream().distinct().count() != 15
                || numbers.stream().anyMatch(number -> number == null || number < 1 || number > 90)) {
            throw new GameDomainException("INVALID_PHYSICAL_CARD_NUMBERS",
                    "El cartón debe contener 15 números únicos entre 1 y 90.");
        }
        List<List<Integer>> byColumn = new ArrayList<>();
        for (int column = 0; column < 9; column++) byColumn.add(new ArrayList<>());
        numbers.forEach(number -> byColumn.get(columnFor(number)).add(number));
        if (byColumn.stream().anyMatch(column -> column.isEmpty() || column.size() > 3)) {
            throw new GameDomainException("INVALID_PHYSICAL_CARD_DISTRIBUTION",
                    "Los números deben cubrir las 9 columnas, con un máximo de 3 por columna.");
        }
        List<List<Integer>> rowChoices = new ArrayList<>();
        if (!chooseRows(byColumn, 0, new int[3], rowChoices)) {
            throw new GameDomainException("INVALID_PHYSICAL_CARD_DISTRIBUTION",
                    "No es posible distribuir esos números en 3 filas de 5.");
        }
        List<List<Integer>> grid = new ArrayList<>();
        for (int row = 0; row < 3; row++) grid.add(new ArrayList<>(java.util.Collections.nCopies(9, null)));
        for (int column = 0; column < 9; column++) {
            List<Integer> values = byColumn.get(column).stream().sorted().toList();
            List<Integer> rows = rowChoices.get(column);
            for (int index = 0; index < values.size(); index++) grid.get(rows.get(index)).set(column, values.get(index));
        }
        return grid.stream().map(row -> java.util.Collections.unmodifiableList(new ArrayList<>(row))).toList();
    }

    private boolean chooseRows(List<List<Integer>> columns, int column, int[] counts,
                               List<List<Integer>> selected) {
        if (column == 9) return counts[0] == 5 && counts[1] == 5 && counts[2] == 5;
        for (List<Integer> choice : combinations(columns.get(column).size())) {
            if (choice.stream().anyMatch(row -> counts[row] >= 5)) continue;
            choice.forEach(row -> counts[row]++);
            selected.add(choice);
            if (chooseRows(columns, column + 1, counts, selected)) return true;
            selected.removeLast();
            choice.forEach(row -> counts[row]--);
        }
        return false;
    }

    private List<List<Integer>> combinations(int size) {
        return switch (size) {
            case 1 -> List.of(List.of(0), List.of(1), List.of(2));
            case 2 -> List.of(List.of(0, 1), List.of(0, 2), List.of(1, 2));
            case 3 -> List.of(List.of(0, 1, 2));
            default -> List.of();
        };
    }

    private int columnFor(int number) {
        return number == 90 ? 8 : number / 10;
    }

    private String sha256(String value) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
