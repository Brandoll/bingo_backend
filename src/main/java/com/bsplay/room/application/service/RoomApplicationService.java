package com.bsplay.room.application.service;

import com.bsplay.room.application.dto.CreateRoomCommand;
import com.bsplay.room.application.dto.JoinRoomCommand;
import com.bsplay.room.application.dto.RoomResponse;
import com.bsplay.room.application.dto.RoomSessionResponse;
import com.bsplay.room.application.dto.StartRoomCommand;
import com.bsplay.room.application.exception.RoomNotFoundException;
import com.bsplay.room.application.mapper.RoomResponseMapper;
import com.bsplay.room.application.port.GuestSessionTokenPort;
import com.bsplay.room.application.port.RoomEventPublisher;
import com.bsplay.room.domain.model.Room;
import com.bsplay.room.domain.repository.RoomRepository;
import com.bsplay.room.domain.service.RoomCodeGenerator;
import com.bsplay.game.application.service.GameApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class RoomApplicationService {
    private final RoomRepository repository;
    private final RoomResponseMapper mapper;
    private final GuestSessionTokenPort tokenPort;
    private final RoomEventPublisher eventPublisher;
    private final RoomCodeGenerator codeGenerator;
    private final Clock clock;
    private final GameApplicationService games;

    public RoomApplicationService(RoomRepository repository, RoomResponseMapper mapper,
                                  GuestSessionTokenPort tokenPort, RoomEventPublisher eventPublisher,
                                  RoomCodeGenerator codeGenerator, Clock clock, GameApplicationService games) {
        this.repository = repository;
        this.mapper = mapper;
        this.tokenPort = tokenPort;
        this.eventPublisher = eventPublisher;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
        this.games = games;
    }

    @Transactional
    public RoomSessionResponse create(CreateRoomCommand command) {
        String code = uniqueCode();
        Room room = Room.create(code, command.roomName(), command.hostName(), command.maxPlayers(), clock.instant());
        Room saved = repository.save(room);
        var host = saved.getMembers().getFirst();
        RoomResponse response = mapper.toResponse(saved);
        eventPublisher.roomCreated(response);
        return new RoomSessionResponse(
                tokenPort.issue(host.getId(), saved.getId(), host.getDisplayName(), host.getRole()),
                host.getId(), host.getRole(), response);
    }

    @Transactional
    public RoomSessionResponse join(JoinRoomCommand command) {
        Room room = repository.findByCodeForUpdate(command.roomCode())
                .orElseThrow(() -> new RoomNotFoundException(command.roomCode()));
        var member = room.join(command.displayName(), clock.instant());
        Room saved = repository.save(room);
        if (saved.getStatus() == com.bsplay.room.domain.model.RoomStatus.RUNNING) {
            games.assignDigitalCardsForActiveGame(saved, member);
        }
        RoomResponse response = mapper.toResponse(saved);
        eventPublisher.playerJoined(response);
        return new RoomSessionResponse(
                tokenPort.issue(member.getId(), saved.getId(), member.getDisplayName(), member.getRole()),
                member.getId(), member.getRole(), response);
    }

    @Transactional(readOnly = true)
    public RoomResponse getByCode(String code) {
        return repository.findByCode(code)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RoomNotFoundException(code));
    }

    @Transactional
    public RoomResponse start(StartRoomCommand command) {
        Room room = repository.findByCodeForUpdate(command.roomCode())
                .orElseThrow(() -> new RoomNotFoundException(command.roomCode()));
        if (!room.getId().equals(command.roomId())) {
            throw new com.bsplay.room.domain.exception.RoomDomainException(
                    "ROOM_ACCESS_DENIED", "La sesión no pertenece a esta sala.");
        }
        room.start(command.memberId());
        Room saved = repository.save(room);
        RoomResponse response = mapper.toResponse(saved);
        games.startForRoom(saved);
        eventPublisher.gameStarted(response);
        return response;
    }

    @Transactional
    public RoomResponse setLocked(String code, boolean locked, java.util.UUID roomId, java.util.UUID memberId) {
        Room room = requireOwnedRoom(code, roomId);
        room.setLocked(memberId, locked);
        RoomResponse response = mapper.toResponse(repository.save(room));
        eventPublisher.roomUpdated(locked ? "ROOM_LOCKED" : "ROOM_UNLOCKED", response);
        return response;
    }

    @Transactional
    public RoomResponse removeMember(String code, java.util.UUID targetMemberId,
                                     java.util.UUID roomId, java.util.UUID actorMemberId) {
        Room room = requireOwnedRoom(code, roomId);
        room.removeMember(actorMemberId, targetMemberId);
        RoomResponse response = mapper.toResponse(repository.save(room));
        eventPublisher.roomUpdated("PLAYER_REMOVED", response);
        return response;
    }

    @Transactional
    public RoomResponse setCoHost(String code, java.util.UUID targetMemberId, boolean enabled,
                                  java.util.UUID roomId, java.util.UUID actorMemberId) {
        Room room = requireOwnedRoom(code, roomId);
        room.setCoHost(actorMemberId, targetMemberId, enabled);
        RoomResponse response = mapper.toResponse(repository.save(room));
        eventPublisher.roomUpdated(enabled ? "CO_HOST_ASSIGNED" : "CO_HOST_REMOVED", response);
        return response;
    }

    @Transactional
    public RoomResponse updateSettings(String code, int cardsPerPlayer, boolean allowLateJoin,
                                       boolean hideParticipantNames, java.util.UUID roomId,
                                       java.util.UUID actorMemberId) {
        Room room = requireOwnedRoom(code, roomId);
        room.updateSettings(actorMemberId, cardsPerPlayer, allowLateJoin, hideParticipantNames);
        RoomResponse response = mapper.toResponse(repository.save(room));
        eventPublisher.roomUpdated("ROOM_SETTINGS_UPDATED", response);
        return response;
    }

    @Transactional
    public RoomResponse close(String code, java.util.UUID roomId, java.util.UUID memberId) {
        Room room = requireOwnedRoom(code, roomId);
        room.close(memberId);
        Room saved = repository.save(room);
        games.closeForRoom(saved.getId());
        RoomResponse response = mapper.toResponse(saved);
        eventPublisher.roomUpdated("ROOM_CLOSED", response);
        return response;
    }

    private Room requireOwnedRoom(String code, java.util.UUID roomId) {
        Room room = repository.findByCodeForUpdate(code)
                .orElseThrow(() -> new RoomNotFoundException(code));
        if (!room.getId().equals(roomId)) {
            throw new com.bsplay.room.domain.exception.RoomDomainException(
                    "ROOM_ACCESS_DENIED", "La sesión no pertenece a esta sala.");
        }
        return room;
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = codeGenerator.nextCode();
            if (repository.findByCode(candidate).isEmpty()) return candidate;
        }
        throw new IllegalStateException("No se pudo reservar un código de sala.");
    }
}
