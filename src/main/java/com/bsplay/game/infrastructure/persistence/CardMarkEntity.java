package com.bsplay.game.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_marks")
public class CardMarkEntity {
    @Id private UUID id;
    @Column(name = "game_card_id", nullable = false) private UUID gameCardId;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Column(nullable = false) private int number;
    @Column(name = "marked_at", nullable = false) private Instant markedAt;

    protected CardMarkEntity() {}

    public CardMarkEntity(UUID gameCardId, UUID memberId, int number, Instant markedAt) {
        this.id = UUID.randomUUID();
        this.gameCardId = gameCardId;
        this.memberId = memberId;
        this.number = number;
        this.markedAt = markedAt;
    }

    public UUID getId() { return id; }
    public int getNumber() { return number; }
}
