package com.bsplay.physicalcard.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
public class PhysicalCardCatalogInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public PhysicalCardCatalogInitializer(JdbcTemplate jdbc, ObjectMapper objectMapper,
            @Value("${bsplay.catalog.enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) return;
        var resource = new ClassPathResource("data/physical-cards.json");
        byte[] bytes = resource.getInputStream().readAllBytes();
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        List<CardSeed> cards = objectMapper.readValue(bytes, new TypeReference<>() {});
        validate(cards);

        Integer alreadyImported = jdbc.queryForObject(
                "select count(*) from physical_card_imports where file_checksum = ? and status = 'COMPLETED'",
                Integer.class, checksum);
        if (alreadyImported != null && alreadyImported > 0) return;

        UUID importId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                insert into physical_card_imports
                (id, file_name, file_checksum, import_version, total_rows, valid_rows, invalid_rows, status, created_at)
                values (?, ?, ?, ?, ?, 0, 0, 'PROCESSING', ?)
                """, importId, "cartas_bingo_completas1.xlsx", checksum, "initial-72", cards.size(),
                Timestamp.from(now));

        for (CardSeed card : cards) {
            String numbersJson = objectMapper.writeValueAsString(card.numbers());
            String gridJson = objectMapper.writeValueAsString(card.grid());
            String structureHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(numbersJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            jdbc.update("""
                    insert into physical_cards
                    (id, external_id, numbers_json, grid_json, structure_hash, source, source_reference,
                     layout_source, created_at, updated_at)
                    values (?, ?, cast(? as jsonb), cast(? as jsonb), ?, 'EXCEL_IMPORT', ?, ?, ?, ?)
                    on conflict (external_id) do update set
                      numbers_json = excluded.numbers_json,
                      grid_json = excluded.grid_json,
                      structure_hash = excluded.structure_hash,
                      source_reference = excluded.source_reference,
                      layout_source = excluded.layout_source,
                      updated_at = excluded.updated_at
                    """, UUID.randomUUID(), card.externalId(), numbersJson, gridJson, structureHash,
                    card.source(), card.layoutSource(), Timestamp.from(now), Timestamp.from(now));
        }
        jdbc.update("""
                update physical_card_imports set valid_rows = ?, status = 'COMPLETED', completed_at = ?
                where id = ?
                """, cards.size(), Timestamp.from(Instant.now()), importId);
    }

    private void validate(List<CardSeed> cards) {
        if (cards.size() != 72) throw new IllegalStateException("El catálogo inicial debe contener 72 cartones.");
        long ids = cards.stream().map(CardSeed::externalId).distinct().count();
        if (ids != cards.size()) throw new IllegalStateException("El catálogo contiene IDs duplicados.");
        for (CardSeed card : cards) {
            if (card.numbers().size() != 15 || card.numbers().stream().distinct().count() != 15
                    || card.numbers().stream().anyMatch(value -> value < 1 || value > 90)) {
                throw new IllegalStateException("Cartón físico inválido: " + card.externalId());
            }
            if (card.grid().size() != 3 || card.grid().stream().anyMatch(row ->
                    row.size() != 9 || row.stream().filter(value -> value != null).count() != 5)) {
                throw new IllegalStateException("Cuadrícula inválida: " + card.externalId());
            }
        }
    }

    private record CardSeed(String externalId, List<Integer> numbers, List<List<Integer>> grid,
                            String source, String layoutSource) {}
}
