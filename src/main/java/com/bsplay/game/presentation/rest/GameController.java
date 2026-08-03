package com.bsplay.game.presentation.rest;

import com.bsplay.game.application.dto.GameCardResponse;
import com.bsplay.game.application.dto.GameSnapshotResponse;
import com.bsplay.game.application.dto.PrizeClaimResponse;
import com.bsplay.game.application.service.GameApplicationService;
import com.bsplay.shared.security.GuestPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{code}/game")
@Validated
public class GameController {
    private final GameApplicationService service;

    public GameController(GameApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public GameSnapshotResponse get(@PathVariable @Pattern(regexp = "[A-Za-z0-9]{6}") String code) {
        return service.getByRoomCode(code);
    }

    @GetMapping("/statistics")
    public com.bsplay.game.application.dto.RoomStatisticsResponse statistics(
            @PathVariable String code, @AuthenticationPrincipal GuestPrincipal principal) {
        return service.getStatistics(code, principal);
    }

    @GetMapping(value = "/statistics/export", produces = "text/csv")
    public ResponseEntity<String> exportStatistics(@PathVariable String code,
                                                   @AuthenticationPrincipal GuestPrincipal principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bsplay-" + code + "-estadisticas.csv")
                .body(service.exportStatistics(code, principal));
    }

    @PostMapping("/draws")
    public GameSnapshotResponse draw(@PathVariable String code, @AuthenticationPrincipal GuestPrincipal principal) {
        return service.draw(code, principal);
    }

    @DeleteMapping("/draws/latest")
    public GameSnapshotResponse undo(@PathVariable String code, @AuthenticationPrincipal GuestPrincipal principal) {
        return service.undo(code, principal);
    }

    @PostMapping("/draws/latest/repeat")
    public GameSnapshotResponse repeat(@PathVariable String code, @AuthenticationPrincipal GuestPrincipal principal) {
        return service.repeat(code, principal);
    }

    @PostMapping("/pause")
    public GameSnapshotResponse pause(@PathVariable String code, @AuthenticationPrincipal GuestPrincipal principal) {
        return service.pause(code, principal);
    }

    @PostMapping("/resume")
    public GameSnapshotResponse resume(@PathVariable String code, @AuthenticationPrincipal GuestPrincipal principal) {
        return service.resume(code, principal);
    }

    @PostMapping("/finish")
    public GameSnapshotResponse finish(@PathVariable String code, @AuthenticationPrincipal GuestPrincipal principal) {
        return service.finish(code, principal);
    }

    @PostMapping("/rounds")
    public GameSnapshotResponse newRound(@PathVariable String code, @AuthenticationPrincipal GuestPrincipal principal) {
        return service.startNewRound(code, principal);
    }

    @PutMapping("/automatic-draw")
    public GameSnapshotResponse automatic(@PathVariable String code,
                                          @Valid @RequestBody AutomaticDrawRequest request,
                                          @AuthenticationPrincipal GuestPrincipal principal) {
        return service.configureAutomatic(code, principal, request.enabled(), request.intervalSeconds());
    }

    @PatchMapping("/settings")
    public GameSnapshotResponse settings(@PathVariable String code,
                                         @RequestBody GameSettingsRequest request,
                                         @AuthenticationPrincipal GuestPrincipal principal) {
        return service.updateSettings(code, principal, request.lineEnabled(), request.doubleLineEnabled(),
                request.bingoEnabled(), request.rankingPublic(), request.automaticBingoDetectionEnabled(),
                request.stopOnBingoEnabled(), request.winnerAnnouncementEnabled());
    }

    @GetMapping("/cards/me")
    public List<GameCardResponse> myCards(@PathVariable String code,
                                         @AuthenticationPrincipal GuestPrincipal principal) {
        return service.getMyCards(code, principal);
    }

    @GetMapping("/cards")
    public List<GameCardResponse> allCards(@PathVariable String code,
                                          @AuthenticationPrincipal GuestPrincipal principal) {
        return service.getAllCards(code, principal);
    }

    @PatchMapping("/cards/{cardId}/marks")
    public GameCardResponse toggleMark(@PathVariable String code, @PathVariable UUID cardId,
                                       @Valid @RequestBody MarkNumberRequest request,
                                       @AuthenticationPrincipal GuestPrincipal principal) {
        return service.toggleMark(code, cardId, request.number(), principal);
    }

    @PostMapping("/claims")
    public PrizeClaimResponse claim(@PathVariable String code, @Valid @RequestBody ClaimPrizeRequest request,
                                    @AuthenticationPrincipal GuestPrincipal principal) {
        return service.claimPrize(code, request.cardId(), request.prizeType(), principal);
    }

    @PostMapping("/claims/{claimId}/review")
    public PrizeClaimResponse review(@PathVariable String code, @PathVariable UUID claimId,
                                     @Valid @RequestBody ReviewClaimRequest request,
                                     @AuthenticationPrincipal GuestPrincipal principal) {
        return service.reviewClaim(code, claimId, request.approved(), request.reason(), principal);
    }

    @PostMapping("/physical-cards")
    public GameCardResponse activatePhysical(@PathVariable String code,
                                             @Valid @RequestBody ActivatePhysicalCardRequest request,
                                             @AuthenticationPrincipal GuestPrincipal principal) {
        return service.activatePhysicalCard(code, request.externalId(), request.displayName(), principal);
    }

    @DeleteMapping("/physical-cards/{cardId}")
    public void deactivatePhysical(@PathVariable String code, @PathVariable UUID cardId,
                                   @AuthenticationPrincipal GuestPrincipal principal) {
        service.deactivatePhysicalCard(code, cardId, principal);
    }
}
