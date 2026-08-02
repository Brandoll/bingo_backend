package com.bsplay.game.infrastructure.persistence;

import com.bsplay.game.domain.exception.GameDomainException;
import com.bsplay.game.domain.model.ClaimStatus;
import com.bsplay.game.domain.model.PrizeType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prize_claims")
public class PrizeClaimEntity {
    @Id private UUID id;
    @Column(name = "game_id", nullable = false) private UUID gameId;
    @Column(name = "game_card_id", nullable = false) private UUID gameCardId;
    @Column(name = "member_id") private UUID memberId;
    @Enumerated(EnumType.STRING) @Column(name = "prize_type", nullable = false, length = 20) private PrizeType prizeType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ClaimStatus status;
    @Column(name = "claimed_at", nullable = false) private Instant claimedAt;
    @Column(name = "validated_at") private Instant validatedAt;
    @Column(name = "validated_by") private UUID validatedBy;
    @Column(name = "rejection_reason", length = 240) private String rejectionReason;

    protected PrizeClaimEntity() {}

    public PrizeClaimEntity(UUID gameId, UUID gameCardId, UUID memberId, PrizeType prizeType, Instant now) {
        this.id = UUID.randomUUID();
        this.gameId = gameId;
        this.gameCardId = gameCardId;
        this.memberId = memberId;
        this.prizeType = prizeType;
        this.status = ClaimStatus.PENDING;
        this.claimedAt = now;
    }

    public void approve(UUID actor, Instant now) {
        requirePending();
        status = ClaimStatus.APPROVED;
        validatedBy = actor;
        validatedAt = now;
    }

    public void reject(UUID actor, String reason, Instant now) {
        requirePending();
        status = ClaimStatus.REJECTED;
        validatedBy = actor;
        validatedAt = now;
        rejectionReason = reason;
    }

    private void requirePending() {
        if (status != ClaimStatus.PENDING) {
            throw new GameDomainException("CLAIM_ALREADY_REVIEWED", "La solicitud ya fue revisada.");
        }
    }

    public UUID getId() { return id; }
    public UUID getGameId() { return gameId; }
    public UUID getGameCardId() { return gameCardId; }
    public UUID getMemberId() { return memberId; }
    public PrizeType getPrizeType() { return prizeType; }
    public ClaimStatus getStatus() { return status; }
    public Instant getClaimedAt() { return claimedAt; }
    public Instant getValidatedAt() { return validatedAt; }
    public UUID getValidatedBy() { return validatedBy; }
    public String getRejectionReason() { return rejectionReason; }
}
