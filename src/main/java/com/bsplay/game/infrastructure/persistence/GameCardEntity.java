package com.bsplay.game.infrastructure.persistence;

import com.bsplay.game.domain.model.CardType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "game_cards")
public class GameCardEntity {
    @Id private UUID id;
    @Column(name = "game_id", nullable = false) private UUID gameId;
    @Column(name = "member_id") private UUID memberId;
    @Column(name = "physical_card_id") private UUID physicalCardId;
    @Enumerated(EnumType.STRING) @Column(name = "card_type", nullable = false, length = 16) private CardType cardType;
    @Column(name = "display_name", nullable = false, length = 40) private String displayName;
    @Column(name = "external_code", nullable = false, length = 32) private String externalCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "grid_json", nullable = false) private List<List<Integer>> grid;
    @Column(name = "is_active", nullable = false) private boolean active;
    @Column(name = "assigned_at", nullable = false) private Instant assignedAt;

    protected GameCardEntity() {}

    public static GameCardEntity digital(UUID gameId, UUID memberId, String displayName, String code,
                                         List<List<Integer>> grid, Instant now) {
        return create(gameId, memberId, null, CardType.DIGITAL, displayName, code, grid, now);
    }

    public static GameCardEntity physical(UUID gameId, UUID physicalCardId, String displayName, String code,
                                          List<List<Integer>> grid, Instant now) {
        return create(gameId, null, physicalCardId, CardType.PHYSICAL, displayName, code, grid, now);
    }

    private static GameCardEntity create(UUID gameId, UUID memberId, UUID physicalCardId, CardType type,
                                         String displayName, String code, List<List<Integer>> grid, Instant now) {
        var card = new GameCardEntity();
        card.id = UUID.randomUUID();
        card.gameId = gameId;
        card.memberId = memberId;
        card.physicalCardId = physicalCardId;
        card.cardType = type;
        card.displayName = displayName;
        card.externalCode = code;
        card.grid = grid;
        card.active = true;
        card.assignedAt = now;
        return card;
    }

    public void deactivate() { active = false; }
    public UUID getId() { return id; }
    public UUID getGameId() { return gameId; }
    public UUID getMemberId() { return memberId; }
    public UUID getPhysicalCardId() { return physicalCardId; }
    public CardType getCardType() { return cardType; }
    public String getDisplayName() { return displayName; }
    public String getExternalCode() { return externalCode; }
    public List<List<Integer>> getGrid() { return grid; }
    public boolean isActive() { return active; }
    public Instant getAssignedAt() { return assignedAt; }
}
