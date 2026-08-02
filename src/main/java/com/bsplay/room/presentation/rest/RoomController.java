package com.bsplay.room.presentation.rest;

import com.bsplay.room.application.dto.CreateRoomCommand;
import com.bsplay.room.application.dto.JoinRoomCommand;
import com.bsplay.room.application.dto.RoomResponse;
import com.bsplay.room.application.dto.RoomSessionResponse;
import com.bsplay.room.application.dto.StartRoomCommand;
import com.bsplay.room.application.service.RoomApplicationService;
import com.bsplay.shared.security.GuestPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@Validated
public class RoomController {
    private final RoomApplicationService service;

    public RoomController(RoomApplicationService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomSessionResponse create(@Valid @RequestBody CreateRoomRequest request) {
        return service.create(new CreateRoomCommand(request.roomName(), request.hostName(), request.maxPlayers()));
    }

    @PostMapping("/{code}/join")
    public RoomSessionResponse join(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9]{6}") String code,
            @Valid @RequestBody JoinRoomRequest request) {
        return service.join(new JoinRoomCommand(code, request.displayName()));
    }

    @GetMapping("/{code}")
    public RoomResponse get(@PathVariable @Pattern(regexp = "[A-Za-z0-9]{6}") String code) {
        return service.getByCode(code);
    }

    @PostMapping("/{code}/start")
    public RoomResponse start(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9]{6}") String code,
            @AuthenticationPrincipal GuestPrincipal principal) {
        return service.start(new StartRoomCommand(code, principal.roomId(), principal.memberId()));
    }

    @PatchMapping("/{code}/lock")
    public RoomResponse lock(@PathVariable String code, @RequestBody LockRoomRequest request,
                             @AuthenticationPrincipal GuestPrincipal principal) {
        return service.setLocked(code, request.locked(), principal.roomId(), principal.memberId());
    }

    @PatchMapping("/{code}/settings")
    public RoomResponse settings(@PathVariable String code, @Valid @RequestBody RoomSettingsRequest request,
                                 @AuthenticationPrincipal GuestPrincipal principal) {
        return service.updateSettings(code, request.cardsPerPlayer(), request.allowLateJoin(),
                request.hideParticipantNames(), principal.roomId(), principal.memberId());
    }

    @PatchMapping("/{code}/members/{memberId}/co-host")
    public RoomResponse coHost(@PathVariable String code, @PathVariable UUID memberId,
                               @RequestBody CoHostRequest request,
                               @AuthenticationPrincipal GuestPrincipal principal) {
        return service.setCoHost(code, memberId, request.enabled(), principal.roomId(), principal.memberId());
    }

    @DeleteMapping("/{code}/members/{memberId}")
    public RoomResponse removeMember(@PathVariable String code, @PathVariable UUID memberId,
                                     @AuthenticationPrincipal GuestPrincipal principal) {
        return service.removeMember(code, memberId, principal.roomId(), principal.memberId());
    }

    @PostMapping("/{code}/close")
    public RoomResponse close(@PathVariable String code, @AuthenticationPrincipal GuestPrincipal principal) {
        return service.close(code, principal.roomId(), principal.memberId());
    }
}
