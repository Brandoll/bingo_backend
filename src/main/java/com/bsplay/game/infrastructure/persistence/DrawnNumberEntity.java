package com.bsplay.game.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drawn_numbers")
public class DrawnNumberEntity {
    @Id private UUID id;
    @Column(name = "game_id", nullable = false) private UUID gameId;
    @Column(nullable = false) private int number;
    @Column(name = "draw_order", nullable = false) private int drawOrder;
    @Column(name = "drawn_by") private UUID drawnBy;
    @Column(name = "drawn_at", nullable = false) private Instant drawnAt;

    protected DrawnNumberEntity() {}

    public DrawnNumberEntity(UUID gameId, int number, int drawOrder, UUID drawnBy, Instant drawnAt) {
        this.id = UUID.randomUUID();
        this.gameId = gameId;
        this.number = number;
        this.drawOrder = drawOrder;
        this.drawnBy = drawnBy;
        this.drawnAt = drawnAt;
    }

    public UUID getId() { return id; }
    public UUID getGameId() { return gameId; }
    public int getNumber() { return number; }
    public int getDrawOrder() { return drawOrder; }
    public UUID getDrawnBy() { return drawnBy; }
    public Instant getDrawnAt() { return drawnAt; }
}
