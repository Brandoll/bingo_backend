package com.bsplay.game.application.service;

import com.bsplay.game.application.dto.*;
import com.bsplay.game.application.port.GameEventPublisher;
import com.bsplay.game.domain.exception.GameDomainException;
import com.bsplay.game.domain.model.*;
import com.bsplay.game.domain.service.BingoCardGenerator;
import com.bsplay.game.domain.service.PrizeEligibility;
import com.bsplay.game.infrastructure.persistence.*;
import com.bsplay.physicalcard.infrastructure.PhysicalCardEntity;
import com.bsplay.physicalcard.infrastructure.PhysicalCardJpaRepository;
import com.bsplay.room.application.exception.RoomNotFoundException;
import com.bsplay.room.domain.model.Room;
import com.bsplay.room.domain.model.RoomMember;
import com.bsplay.room.domain.repository.RoomRepository;
import com.bsplay.shared.security.GuestPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GameApplicationService {
    private final GameJpaRepository games;
    private final DrawnNumberJpaRepository draws;
    private final GameCardJpaRepository cards;
    private final CardMarkJpaRepository marks;
    private final PrizeClaimJpaRepository claims;
    private final PhysicalCardJpaRepository physicalCards;
    private final RoomRepository rooms;
    private final GameEventPublisher events;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final BingoCardGenerator cardGenerator = new BingoCardGenerator();

    public GameApplicationService(GameJpaRepository games, DrawnNumberJpaRepository draws,
                                  GameCardJpaRepository cards, CardMarkJpaRepository marks,
                                  PrizeClaimJpaRepository claims, PhysicalCardJpaRepository physicalCards,
                                  RoomRepository rooms, GameEventPublisher events, Clock clock) {
        this.games = games;
        this.draws = draws;
        this.cards = cards;
        this.marks = marks;
        this.claims = claims;
        this.physicalCards = physicalCards;
        this.rooms = rooms;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public GameSnapshotResponse startForRoom(Room room) {
        var previous = games.findTopByRoomIdOrderByRoundNumberDesc(room.getId());
        if (previous.isPresent() && previous.get().getStatus() != GameStatus.ROUND_FINISHED
                && previous.get().getStatus() != GameStatus.CLOSED) {
            throw new GameDomainException("GAME_ALREADY_STARTED", "La partida ya está iniciada.");
        }
        int round = previous.map(game -> game.getRoundNumber() + 1).orElse(1);
        GameEntity game = games.save(GameEntity.create(room.getId(), round, clock.instant()));
        assignDigitalCards(game, room);
        GameSnapshotResponse snapshot = toSnapshot(game);
        events.publish("GAME_STARTED", snapshot);
        return snapshot;
    }

    @Transactional
    public void assignDigitalCardsForActiveGame(Room room, RoomMember member) {
        GameEntity game = games.findFirstByRoomIdOrderByRoundNumberDesc(room.getId()).orElse(null);
        if (game == null || game.getStatus() == GameStatus.ROUND_FINISHED || game.getStatus() == GameStatus.CLOSED
                || cards.existsByGameIdAndMemberIdAndActiveTrue(game.getId(), member.getId())) return;
        assignDigitalCards(game, member, room.getCardsPerPlayer());
        events.publish("CARD_ASSIGNED", toSnapshot(game));
    }

    @Transactional
    public void closeForRoom(UUID roomId) {
        games.findFirstByRoomIdOrderByRoundNumberDesc(roomId).ifPresent(game -> {
            game.close(clock.instant());
            games.save(game);
            events.publish("ROOM_CLOSED", toSnapshot(game));
        });
    }

    @Transactional(readOnly = true)
    public GameSnapshotResponse getByRoomCode(String code) {
        Room room = requireRoom(code);
        return games.findTopByRoomIdOrderByRoundNumberDesc(room.getId())
                .map(this::toSnapshot)
                .orElseThrow(() -> new GameDomainException("GAME_NOT_STARTED", "La sala todavía no tiene una partida."));
    }

    @Transactional(readOnly = true)
    public RoomStatisticsResponse getStatistics(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        List<RoundStatisticsResponse> rounds = games.findByRoomIdOrderByRoundNumberDesc(room.getId()).stream()
                .map(game -> new RoundStatisticsResponse(game.getId(), game.getRoundNumber(), game.getStatus(),
                        game.getCurrentDrawOrder(), cards.countByGameId(game.getId()),
                        cards.countByGameIdAndCardType(game.getId(), CardType.PHYSICAL),
                        claims.countByGameIdAndStatus(game.getId(), ClaimStatus.APPROVED),
                        game.getStartedAt(), game.getEndedAt())).toList();
        return new RoomStatisticsResponse(room.getId(), rounds.size(),
                rounds.stream().mapToLong(RoundStatisticsResponse::drawnNumbers).sum(),
                rounds.stream().mapToLong(RoundStatisticsResponse::assignedCards).sum(),
                rounds.stream().mapToLong(RoundStatisticsResponse::physicalCards).sum(),
                rounds.stream().mapToLong(RoundStatisticsResponse::approvedPrizes).sum(), rounds);
    }

    @Transactional(readOnly = true)
    public String exportStatistics(String code, GuestPrincipal principal) {
        RoomStatisticsResponse stats = getStatistics(code, principal);
        StringBuilder csv = new StringBuilder("ronda,estado,bolas,cartones,cartones_fisicos,premios,inicio,fin\n");
        for (RoundStatisticsResponse round : stats.rounds()) {
            csv.append(round.roundNumber()).append(',').append(round.status()).append(',')
                    .append(round.drawnNumbers()).append(',').append(round.assignedCards()).append(',')
                    .append(round.physicalCards()).append(',').append(round.approvedPrizes()).append(',')
                    .append(round.startedAt()).append(',').append(round.endedAt() == null ? "" : round.endedAt()).append('\n');
        }
        return csv.toString();
    }

    @Transactional
    public GameSnapshotResponse draw(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        return drawInternal(game, principal.memberId(), clock.instant(), "NUMBER_DRAWN");
    }

    @Transactional
    public void drawAutomatic(UUID gameId) {
        GameEntity game = games.findByIdForUpdate(gameId).orElse(null);
        Instant now = clock.instant();
        if (game == null || !game.isAutomaticDrawEnabled() || game.getStatus() != GameStatus.RUNNING
                || game.getNextAutomaticDrawAt() == null || game.getNextAutomaticDrawAt().isAfter(now)) return;
        drawInternal(game, null, now, "NUMBER_DRAWN");
    }

    @Transactional(readOnly = true)
    public List<UUID> automaticGamesDue() {
        return games.findAutomaticGamesDue(clock.instant());
    }

    @Transactional
    public GameSnapshotResponse undo(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        DrawnNumberEntity latest = draws.findTopByGameIdOrderByDrawOrderDesc(game.getId())
                .orElseThrow(() -> new GameDomainException("NO_DRAW_TO_UNDO", "Todavía no hay bolas para deshacer."));
        game.undoDraw();
        draws.delete(latest);
        games.save(game);
        GameSnapshotResponse snapshot = toSnapshot(game);
        events.publish("DRAW_UNDONE", snapshot);
        return snapshot;
    }

    @Transactional
    public GameSnapshotResponse repeat(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        draws.findTopByGameIdOrderByDrawOrderDesc(game.getId())
                .orElseThrow(() -> new GameDomainException("NO_DRAW_TO_REPEAT", "Todavía no hay una bola que repetir."));
        GameSnapshotResponse snapshot = toSnapshot(game);
        events.publish("NUMBER_REPEATED", snapshot);
        return snapshot;
    }

    @Transactional
    public GameSnapshotResponse pause(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        game.pause(clock.instant());
        GameSnapshotResponse snapshot = toSnapshot(games.save(game));
        events.publish("GAME_PAUSED", snapshot);
        return snapshot;
    }

    @Transactional
    public GameSnapshotResponse resume(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        game.resume();
        GameSnapshotResponse snapshot = toSnapshot(games.save(game));
        events.publish("GAME_RESUMED", snapshot);
        return snapshot;
    }

    @Transactional
    public GameSnapshotResponse finish(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        game.finish(clock.instant());
        GameSnapshotResponse snapshot = toSnapshot(games.save(game));
        events.publish("ROUND_FINISHED", snapshot);
        return snapshot;
    }

    @Transactional
    public GameSnapshotResponse startNewRound(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity previous = latestForUpdate(room.getId());
        if (previous.getStatus() != GameStatus.ROUND_FINISHED) {
            throw new GameDomainException("ROUND_NOT_FINISHED", "Finaliza la ronda actual antes de iniciar otra.");
        }
        GameEntity game = games.save(GameEntity.create(room.getId(), previous.getRoundNumber() + 1, clock.instant()));
        assignDigitalCards(game, room);
        GameSnapshotResponse snapshot = toSnapshot(game);
        events.publish("GAME_STARTED", snapshot);
        return snapshot;
    }

    @Transactional
    public GameSnapshotResponse configureAutomatic(String code, GuestPrincipal principal,
                                                    boolean enabled, int intervalSeconds) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        game.configureAutomatic(enabled, intervalSeconds, clock.instant());
        GameSnapshotResponse snapshot = toSnapshot(games.save(game));
        events.publish("AUTOMATIC_DRAW_CHANGED", snapshot);
        return snapshot;
    }

    @Transactional
    public GameSnapshotResponse updateSettings(String code, GuestPrincipal principal,
                                               boolean lineEnabled, boolean doubleLineEnabled,
                                               boolean bingoEnabled, boolean rankingPublic) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        game.updateSettings(lineEnabled, doubleLineEnabled, bingoEnabled, rankingPublic);
        GameSnapshotResponse snapshot = toSnapshot(games.save(game));
        events.publish("GAME_SETTINGS_UPDATED", snapshot);
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<GameCardResponse> getMyCards(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, false);
        GameEntity game = latest(room.getId());
        Set<Integer> drawn = drawnSet(game.getId());
        return cards.findByGameIdAndMemberIdAndActiveTrue(game.getId(), principal.memberId()).stream()
                .map(card -> toCardResponse(card, drawn)).toList();
    }

    @Transactional(readOnly = true)
    public List<GameCardResponse> getAllCards(String code, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latest(room.getId());
        Set<Integer> drawn = drawnSet(game.getId());
        return cards.findByGameIdAndActiveTrueOrderByAssignedAtAsc(game.getId()).stream()
                .map(card -> toCardResponse(card, drawn)).toList();
    }

    @Transactional
    public GameCardResponse toggleMark(String code, UUID cardId, int number, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, false);
        GameEntity game = latestForUpdate(room.getId());
        GameCardEntity card = cards.findByIdAndGameId(cardId, game.getId())
                .orElseThrow(() -> new GameDomainException("CARD_NOT_FOUND", "No encontramos ese cartón."));
        boolean canManage = Objects.equals(card.getMemberId(), principal.memberId()) || isController(room, principal.memberId());
        if (!canManage) throw new GameDomainException("CARD_ACCESS_DENIED", "Ese cartón pertenece a otro jugador.");
        if (number < 1 || number > 90 || card.getGrid().stream().flatMap(List::stream)
                .filter(Objects::nonNull).noneMatch(value -> value == number)) {
            throw new GameDomainException("NUMBER_NOT_ON_CARD", "Ese número no pertenece al cartón.");
        }
        if (!draws.existsByGameIdAndNumber(game.getId(), number)) {
            throw new GameDomainException("NUMBER_NOT_DRAWN", "Sólo puedes marcar números que ya salieron.");
        }
        marks.findByGameCardIdAndNumber(cardId, number).ifPresentOrElse(marks::delete,
                () -> marks.save(new CardMarkEntity(cardId, principal.memberId(), number, clock.instant())));
        return toCardResponse(card, drawnSet(game.getId()));
    }

    @Transactional
    public PrizeClaimResponse claimPrize(String code, UUID cardId, PrizeType type, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, false);
        GameEntity game = latestForUpdate(room.getId());
        GameCardEntity card = cards.findByIdAndGameId(cardId, game.getId())
                .orElseThrow(() -> new GameDomainException("CARD_NOT_FOUND", "No encontramos ese cartón."));
        if (!Objects.equals(card.getMemberId(), principal.memberId()) && !isController(room, principal.memberId())) {
            throw new GameDomainException("CARD_ACCESS_DENIED", "Ese cartón pertenece a otro jugador.");
        }
        if (!game.isPrizeEnabled(type)) {
            throw new GameDomainException("PRIZE_DISABLED", "Ese premio no está habilitado en esta ronda.");
        }
        if (!PrizeEligibility.isEligible(card.getGrid(), drawnSet(game.getId()), type)) {
            throw new GameDomainException("PRIZE_NOT_EARNED", "El cartón todavía no cumple ese premio.");
        }
        if (claims.existsByGameIdAndGameCardIdAndPrizeTypeAndStatus(game.getId(), cardId, type, ClaimStatus.PENDING)
                || claims.existsByGameIdAndGameCardIdAndPrizeTypeAndStatus(game.getId(), cardId, type, ClaimStatus.APPROVED)) {
            throw new GameDomainException("CLAIM_ALREADY_EXISTS", "Ese premio ya fue solicitado para el cartón.");
        }
        PrizeClaimEntity claim = claims.save(new PrizeClaimEntity(game.getId(), cardId, card.getMemberId(), type, clock.instant()));
        GameSnapshotResponse snapshot = toSnapshot(game);
        events.publish("PRIZE_CLAIMED", snapshot);
        return toClaimResponse(claim, card);
    }

    @Transactional
    public PrizeClaimResponse reviewClaim(String code, UUID claimId, boolean approved, String reason,
                                          GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        PrizeClaimEntity claim = claims.findById(claimId)
                .orElseThrow(() -> new GameDomainException("CLAIM_NOT_FOUND", "No encontramos esa solicitud."));
        GameEntity game = games.findByIdForUpdate(claim.getGameId())
                .orElseThrow(() -> new GameDomainException("GAME_NOT_FOUND", "No encontramos la partida."));
        if (!game.getRoomId().equals(room.getId())) {
            throw new GameDomainException("ROOM_ACCESS_DENIED", "La solicitud no pertenece a esta sala.");
        }
        GameCardEntity card = cards.findById(claim.getGameCardId())
                .orElseThrow(() -> new GameDomainException("CARD_NOT_FOUND", "No encontramos el cartón reclamado."));
        if (approved) {
            if (!PrizeEligibility.isEligible(card.getGrid(), drawnSet(game.getId()), claim.getPrizeType())) {
                throw new GameDomainException("PRIZE_NOT_EARNED", "El cartón no cumple el premio solicitado.");
            }
            claim.approve(principal.memberId(), clock.instant());
            if (claim.getPrizeType() == PrizeType.BINGO) game.finish(clock.instant());
        } else {
            claim.reject(principal.memberId(), reason == null ? "Solicitud rechazada por el host." : reason.trim(), clock.instant());
        }
        claims.save(claim);
        games.save(game);
        GameSnapshotResponse snapshot = toSnapshot(game);
        events.publish(approved ? "PRIZE_APPROVED" : "PRIZE_REJECTED", snapshot);
        return toClaimResponse(claim, card);
    }

    @Transactional(readOnly = true)
    public PhysicalCardResponse findPhysicalCard(String externalId) {
        PhysicalCardEntity card = physicalCards.findByExternalIdIgnoreCase(externalId)
                .orElseThrow(() -> new GameDomainException("PHYSICAL_CARD_NOT_FOUND", "No encontramos esa cartilla física."));
        return new PhysicalCardResponse(card.getId(), card.getExternalId(), card.getNumbers(), card.getGrid());
    }

    @Transactional
    public GameCardResponse activatePhysicalCard(String code, String externalId, String displayName,
                                                 GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        if (game.getStatus() == GameStatus.ROUND_FINISHED || game.getStatus() == GameStatus.CLOSED) {
            throw new GameDomainException("GAME_ALREADY_FINISHED", "No puedes activar cartones en una ronda finalizada.");
        }
        PhysicalCardEntity physical = physicalCards.findByExternalIdIgnoreCase(externalId)
                .orElseThrow(() -> new GameDomainException("PHYSICAL_CARD_NOT_FOUND", "No encontramos esa cartilla física."));
        if (cards.findByGameIdAndPhysicalCardIdAndActiveTrue(game.getId(), physical.getId()).isPresent()) {
            throw new GameDomainException("PHYSICAL_CARD_ALREADY_ACTIVE", "La cartilla física ya está activa en esta ronda.");
        }
        GameCardEntity card = cards.save(GameCardEntity.physical(game.getId(), physical.getId(), displayName.trim(),
                physical.getExternalId(), physical.getGrid(), clock.instant()));
        GameSnapshotResponse snapshot = toSnapshot(game);
        events.publish("PHYSICAL_CARD_ACTIVATED", snapshot);
        return toCardResponse(card, drawnSet(game.getId()));
    }

    @Transactional
    public void deactivatePhysicalCard(String code, UUID cardId, GuestPrincipal principal) {
        Room room = requireRoomAndPrincipal(code, principal, true);
        GameEntity game = latestForUpdate(room.getId());
        GameCardEntity card = cards.findByIdAndGameId(cardId, game.getId())
                .orElseThrow(() -> new GameDomainException("CARD_NOT_FOUND", "No encontramos ese cartón."));
        if (card.getCardType() != CardType.PHYSICAL) {
            throw new GameDomainException("NOT_A_PHYSICAL_CARD", "El cartón seleccionado no es físico.");
        }
        card.deactivate();
        cards.save(card);
        events.publish("PHYSICAL_CARD_DEACTIVATED", toSnapshot(game));
    }

    private GameSnapshotResponse drawInternal(GameEntity game, UUID actor, Instant now, String eventType) {
        Set<Integer> alreadyDrawn = drawnSet(game.getId());
        List<Integer> remaining = new ArrayList<>();
        for (int number = 1; number <= 90; number++) if (!alreadyDrawn.contains(number)) remaining.add(number);
        if (remaining.isEmpty()) throw new GameDomainException("DRAW_POOL_EMPTY", "Ya se extrajeron las 90 bolas.");
        int number = remaining.get(random.nextInt(remaining.size()));
        int order = game.registerDraw();
        draws.save(new DrawnNumberEntity(game.getId(), number, order, actor, now));
        game.scheduleNextAutomaticDraw(now);
        games.save(game);
        GameSnapshotResponse snapshot = toSnapshot(game);
        events.publish(eventType, snapshot);
        return snapshot;
    }

    private void assignDigitalCards(GameEntity game, Room room) {
        for (var member : room.getMembers()) {
            assignDigitalCards(game, member, room.getCardsPerPlayer());
        }
    }

    private void assignDigitalCards(GameEntity game, RoomMember member, int quantity) {
        for (int index = 1; index <= quantity; index++) {
            GeneratedCard generated = cardGenerator.generate();
            String code = "D" + game.getRoundNumber() + "-"
                    + member.getId().toString().substring(0, 6).toUpperCase(Locale.ROOT) + "-" + index;
            cards.save(GameCardEntity.digital(game.getId(), member.getId(), member.getDisplayName(), code,
                    generated.grid(), clock.instant()));
        }
    }

    private Room requireRoom(String code) {
        return rooms.findByCode(code).orElseThrow(() -> new RoomNotFoundException(code));
    }

    private Room requireRoomAndPrincipal(String code, GuestPrincipal principal, boolean hostRequired) {
        if (principal == null) throw new GameDomainException("AUTH_REQUIRED", "Debes volver a entrar a la sala.");
        Room room = requireRoom(code);
        if (!room.getId().equals(principal.roomId())) {
            throw new GameDomainException("ROOM_ACCESS_DENIED", "La sesión no pertenece a esta sala.");
        }
        if (hostRequired && room.getMembers().stream().noneMatch(member ->
                member.getId().equals(principal.memberId())
                        && (member.getRole() == com.bsplay.room.domain.model.MemberRole.HOST
                        || member.getRole() == com.bsplay.room.domain.model.MemberRole.CO_HOST))) {
            throw new GameDomainException("CONTROLLER_REQUIRED", "Se requieren permisos de anfitrión.");
        }
        return room;
    }

    private boolean isController(Room room, UUID memberId) {
        return room.getMembers().stream().anyMatch(member -> member.getId().equals(memberId)
                && (member.getRole() == com.bsplay.room.domain.model.MemberRole.HOST
                || member.getRole() == com.bsplay.room.domain.model.MemberRole.CO_HOST));
    }

    private GameEntity latest(UUID roomId) {
        return games.findTopByRoomIdOrderByRoundNumberDesc(roomId)
                .orElseThrow(() -> new GameDomainException("GAME_NOT_STARTED", "La sala todavía no tiene una partida."));
    }

    private GameEntity latestForUpdate(UUID roomId) {
        return games.findFirstByRoomIdOrderByRoundNumberDesc(roomId)
                .orElseThrow(() -> new GameDomainException("GAME_NOT_STARTED", "La sala todavía no tiene una partida."));
    }

    private Set<Integer> drawnSet(UUID gameId) {
        return draws.findByGameIdOrderByDrawOrderAsc(gameId).stream()
                .map(DrawnNumberEntity::getNumber).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private GameSnapshotResponse toSnapshot(GameEntity game) {
        List<Integer> drawn = draws.findByGameIdOrderByDrawOrderAsc(game.getId()).stream()
                .map(DrawnNumberEntity::getNumber).toList();
        Set<Integer> drawnSet = new HashSet<>(drawn);
        List<GameCardEntity> gameCards = cards.findByGameIdAndActiveTrueOrderByAssignedAtAsc(game.getId());
        List<RankingEntryResponse> ranking = gameCards.stream().map(card -> toRanking(card, drawnSet))
                .sorted(Comparator.comparingInt(RankingEntryResponse::matchedNumbers).reversed()
                        .thenComparing(RankingEntryResponse::displayName))
                .limit(10).toList();
        Map<UUID, GameCardEntity> cardsById = gameCards.stream()
                .collect(Collectors.toMap(GameCardEntity::getId, Function.identity()));
        List<PrizeClaimResponse> claimResponses = claims.findByGameIdOrderByClaimedAtDesc(game.getId()).stream()
                .map(claim -> toClaimResponse(claim, cardsById.get(claim.getGameCardId())))
                .filter(Objects::nonNull).toList();
        Integer current = drawn.isEmpty() ? null : drawn.getLast();
        Integer previous = drawn.size() < 2 ? null : drawn.get(drawn.size() - 2);
        return new GameSnapshotResponse(game.getId(), game.getRoomId(), game.getRoundNumber(), game.getStatus(),
                current, previous, drawn, 90 - drawn.size(), game.isAutomaticDrawEnabled(),
                game.getAutomaticDrawIntervalSeconds(), game.isLineEnabled(), game.isDoubleLineEnabled(),
                game.isBingoEnabled(), game.isRankingPublic(), game.getStartedAt(), game.getPausedAt(),
                game.getEndedAt(), ranking, claimResponses);
    }

    private GameCardResponse toCardResponse(GameCardEntity card, Set<Integer> drawn) {
        List<Integer> marked = marks.findByGameCardId(card.getId()).stream().map(CardMarkEntity::getNumber).sorted().toList();
        RankingEntryResponse progress = toRanking(card, drawn);
        return new GameCardResponse(card.getId(), card.getCardType(), card.getDisplayName(), card.getExternalCode(),
                card.getGrid(), marked, progress.matchedNumbers(), progress.completedRows(), card.isActive());
    }

    private RankingEntryResponse toRanking(GameCardEntity card, Set<Integer> drawn) {
        List<Integer> numbers = card.getGrid().stream().flatMap(List::stream).filter(Objects::nonNull).toList();
        int matched = (int) numbers.stream().filter(drawn::contains).count();
        int completedRows = (int) card.getGrid().stream()
                .filter(row -> row.stream().filter(Objects::nonNull).allMatch(drawn::contains)).count();
        return new RankingEntryResponse(card.getId(), card.getDisplayName(), card.getExternalCode(), matched,
                completedRows, 15 - matched);
    }

    private PrizeClaimResponse toClaimResponse(PrizeClaimEntity claim, GameCardEntity card) {
        if (card == null) return null;
        return new PrizeClaimResponse(claim.getId(), card.getId(), card.getDisplayName(), card.getExternalCode(),
                claim.getPrizeType(), claim.getStatus(), claim.getClaimedAt(), claim.getValidatedAt(),
                claim.getRejectionReason());
    }
}
