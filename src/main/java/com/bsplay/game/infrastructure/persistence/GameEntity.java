package com.bsplay.game.infrastructure.persistence;

import com.bsplay.game.domain.exception.GameDomainException;
import com.bsplay.game.domain.model.GameStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "games")
public class GameEntity {
    @Id private UUID id;
    @Column(name = "room_id", nullable = false) private UUID roomId;
    @Column(name = "round_number", nullable = false) private int roundNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private GameStatus status;
    @Column(name = "current_draw_order", nullable = false) private int currentDrawOrder;
    @Column(name = "automatic_draw_enabled", nullable = false) private boolean automaticDrawEnabled;
    @Column(name = "automatic_draw_interval_seconds", nullable = false) private int automaticDrawIntervalSeconds;
    @Column(name = "next_automatic_draw_at") private Instant nextAutomaticDrawAt;
    @Column(name = "line_enabled", nullable = false) private boolean lineEnabled;
    @Column(name = "double_line_enabled", nullable = false) private boolean doubleLineEnabled;
    @Column(name = "bingo_enabled", nullable = false) private boolean bingoEnabled;
    @Column(name = "ranking_public", nullable = false) private boolean rankingPublic;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "paused_at") private Instant pausedAt;
    @Column(name = "ended_at") private Instant endedAt;
    @Version @Column(nullable = false) private long version;

    protected GameEntity() {}

    public static GameEntity create(UUID roomId, int roundNumber, Instant now) {
        var game = new GameEntity();
        game.id = UUID.randomUUID();
        game.roomId = roomId;
        game.roundNumber = roundNumber;
        game.status = GameStatus.RUNNING;
        game.currentDrawOrder = 0;
        game.automaticDrawIntervalSeconds = 8;
        game.lineEnabled = true;
        game.doubleLineEnabled = true;
        game.bingoEnabled = true;
        game.rankingPublic = true;
        game.startedAt = now;
        return game;
    }

    public int registerDraw() {
        requireStatus(GameStatus.RUNNING, "GAME_NOT_RUNNING", "La partida debe estar en curso para extraer una bola.");
        if (currentDrawOrder >= 90) throw new GameDomainException("DRAW_POOL_EMPTY", "Ya se extrajeron las 90 bolas.");
        return ++currentDrawOrder;
    }

    public void undoDraw() {
        requireStatus(GameStatus.RUNNING, "GAME_NOT_RUNNING", "Sólo se puede deshacer durante la partida.");
        if (currentDrawOrder == 0) throw new GameDomainException("NO_DRAW_TO_UNDO", "Todavía no hay bolas para deshacer.");
        currentDrawOrder--;
    }

    public void pause(Instant now) {
        requireStatus(GameStatus.RUNNING, "GAME_NOT_RUNNING", "La partida no está en curso.");
        status = GameStatus.PAUSED;
        pausedAt = now;
        automaticDrawEnabled = false;
        nextAutomaticDrawAt = null;
    }

    public void resume() {
        requireStatus(GameStatus.PAUSED, "GAME_NOT_PAUSED", "La partida no está pausada.");
        status = GameStatus.RUNNING;
        pausedAt = null;
    }

    public void finish(Instant now) {
        if (status != GameStatus.RUNNING && status != GameStatus.PAUSED && status != GameStatus.VALIDATING_PRIZE) {
            throw new GameDomainException("GAME_ALREADY_FINISHED", "La ronda ya terminó.");
        }
        status = GameStatus.ROUND_FINISHED;
        endedAt = now;
        automaticDrawEnabled = false;
        nextAutomaticDrawAt = null;
    }

    public void close(Instant now) {
        status = GameStatus.CLOSED;
        endedAt = endedAt == null ? now : endedAt;
        automaticDrawEnabled = false;
        nextAutomaticDrawAt = null;
    }

    public void configureAutomatic(boolean enabled, int intervalSeconds, Instant now) {
        if (intervalSeconds < 3 || intervalSeconds > 60) {
            throw new GameDomainException("INVALID_DRAW_INTERVAL", "El intervalo debe estar entre 3 y 60 segundos.");
        }
        requireStatus(GameStatus.RUNNING, "GAME_NOT_RUNNING", "La partida debe estar en curso.");
        automaticDrawIntervalSeconds = intervalSeconds;
        automaticDrawEnabled = enabled;
        nextAutomaticDrawAt = enabled ? now.plusSeconds(intervalSeconds) : null;
    }

    public void scheduleNextAutomaticDraw(Instant now) {
        if (currentDrawOrder >= 90) automaticDrawEnabled = false;
        nextAutomaticDrawAt = automaticDrawEnabled ? now.plusSeconds(automaticDrawIntervalSeconds) : null;
    }

    public void updateSettings(boolean lineEnabled, boolean doubleLineEnabled, boolean bingoEnabled,
                               boolean rankingPublic) {
        this.lineEnabled = lineEnabled;
        this.doubleLineEnabled = doubleLineEnabled;
        this.bingoEnabled = bingoEnabled;
        this.rankingPublic = rankingPublic;
    }

    public boolean isPrizeEnabled(com.bsplay.game.domain.model.PrizeType type) {
        return switch (type) {
            case LINE -> lineEnabled;
            case DOUBLE_LINE -> doubleLineEnabled;
            case BINGO -> bingoEnabled;
        };
    }

    private void requireStatus(GameStatus expected, String code, String message) {
        if (status != expected) throw new GameDomainException(code, message);
    }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public int getRoundNumber() { return roundNumber; }
    public GameStatus getStatus() { return status; }
    public int getCurrentDrawOrder() { return currentDrawOrder; }
    public boolean isAutomaticDrawEnabled() { return automaticDrawEnabled; }
    public int getAutomaticDrawIntervalSeconds() { return automaticDrawIntervalSeconds; }
    public Instant getNextAutomaticDrawAt() { return nextAutomaticDrawAt; }
    public boolean isLineEnabled() { return lineEnabled; }
    public boolean isDoubleLineEnabled() { return doubleLineEnabled; }
    public boolean isBingoEnabled() { return bingoEnabled; }
    public boolean isRankingPublic() { return rankingPublic; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getPausedAt() { return pausedAt; }
    public Instant getEndedAt() { return endedAt; }
}
