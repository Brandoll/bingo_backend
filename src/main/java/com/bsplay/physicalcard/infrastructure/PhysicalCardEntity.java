package com.bsplay.physicalcard.infrastructure;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "physical_cards")
public class PhysicalCardEntity {
    @Id private UUID id;
    @Column(name = "external_id", nullable = false) private String externalId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "numbers_json", nullable = false) private List<Integer> numbers;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "grid_json", nullable = false) private List<List<Integer>> grid;

    protected PhysicalCardEntity() {}

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public List<Integer> getNumbers() { return numbers; }
    public List<List<Integer>> getGrid() { return grid; }
}
