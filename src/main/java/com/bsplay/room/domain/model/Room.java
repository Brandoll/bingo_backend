package com.bsplay.room.domain.model;

import com.bsplay.room.domain.exception.RoomDomainException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Room {
    private final UUID id;
    private final String code;
    private final String name;
    private RoomStatus status;
    private boolean locked;
    private final int maxPlayers;
    private int cardsPerPlayer;
    private boolean allowLateJoin;
    private boolean hideParticipantNames;
    private final Instant createdAt;
    private long version;
    private final List<RoomMember> members;

    public Room(UUID id, String code, String name, RoomStatus status, boolean locked,
                int maxPlayers, int cardsPerPlayer, boolean allowLateJoin, boolean hideParticipantNames,
                Instant createdAt, long version, List<RoomMember> members) {
        this.id = Objects.requireNonNull(id);
        this.code = Objects.requireNonNull(code).toUpperCase(Locale.ROOT);
        this.name = Objects.requireNonNull(name);
        this.status = Objects.requireNonNull(status);
        this.locked = locked;
        this.maxPlayers = maxPlayers;
        this.cardsPerPlayer = cardsPerPlayer;
        this.allowLateJoin = allowLateJoin;
        this.hideParticipantNames = hideParticipantNames;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.version = version;
        this.members = new ArrayList<>(members);
    }

    public static Room create(String code, String name, String hostName, int maxPlayers, Instant now) {
        var host = new RoomMember(UUID.randomUUID(), hostName.trim(), MemberRole.HOST,
                ConnectionStatus.CONNECTED, now);
        return new Room(UUID.randomUUID(), code, name.trim(), RoomStatus.WAITING, false,
                maxPlayers, 1, true, false, now, 0, List.of(host));
    }

    public RoomMember join(String displayName, Instant now) {
        if (status == RoomStatus.CLOSED) {
            throw new RoomDomainException("ROOM_NOT_JOINABLE", "La sala ya no admite jugadores.");
        }
        if (locked) {
            throw new RoomDomainException("ROOM_LOCKED", "La sala está bloqueada.");
        }
        if (status == RoomStatus.RUNNING && !allowLateJoin) {
            throw new RoomDomainException("LATE_JOIN_DISABLED", "La sala no admite entradas durante la partida.");
        }
        if (members.size() >= maxPlayers) {
            throw new RoomDomainException("ROOM_FULL", "La sala alcanzó su capacidad máxima.");
        }
        String normalizedName = displayName.trim();
        boolean repeated = members.stream().anyMatch(member ->
                member.getDisplayName().equalsIgnoreCase(normalizedName));
        if (repeated) {
            throw new RoomDomainException("DISPLAY_NAME_TAKEN", "Ese nombre ya está en uso en la sala.");
        }
        var member = new RoomMember(UUID.randomUUID(), normalizedName, MemberRole.PLAYER,
                ConnectionStatus.CONNECTED, now);
        members.add(member);
        return member;
    }

    public void start(UUID actorMemberId) {
        if (status != RoomStatus.WAITING) {
            throw new RoomDomainException("ROOM_ALREADY_STARTED", "La partida ya fue iniciada.");
        }
        boolean isHost = members.stream().anyMatch(member ->
                member.getId().equals(actorMemberId) && member.getRole() == MemberRole.HOST);
        if (!isHost) {
            throw new RoomDomainException("HOST_REQUIRED", "Sólo el host puede iniciar la partida.");
        }
        status = RoomStatus.RUNNING;
    }

    public void setLocked(UUID actorMemberId, boolean locked) {
        requireHost(actorMemberId);
        if (status == RoomStatus.CLOSED) throw new RoomDomainException("ROOM_CLOSED", "La sala está cerrada.");
        this.locked = locked;
    }

    public void removeMember(UUID actorMemberId, UUID memberId) {
        requireController(actorMemberId);
        RoomMember target = members.stream().filter(member -> member.getId().equals(memberId)).findFirst()
                .orElseThrow(() -> new RoomDomainException("MEMBER_NOT_FOUND", "No encontramos ese jugador."));
        if (target.getRole() == MemberRole.HOST) {
            throw new RoomDomainException("HOST_CANNOT_BE_REMOVED", "El anfitrión no puede expulsarse de su sala.");
        }
        members.remove(target);
    }

    public void close(UUID actorMemberId) {
        requireHost(actorMemberId);
        status = RoomStatus.CLOSED;
        locked = true;
    }

    public void setCoHost(UUID actorMemberId, UUID memberId, boolean enabled) {
        requireHost(actorMemberId);
        RoomMember target = members.stream().filter(member -> member.getId().equals(memberId)).findFirst()
                .orElseThrow(() -> new RoomDomainException("MEMBER_NOT_FOUND", "No encontramos ese jugador."));
        if (target.getRole() == MemberRole.HOST) {
            throw new RoomDomainException("HOST_ROLE_IMMUTABLE", "El anfitrión principal conserva su rol.");
        }
        target.setRole(enabled ? MemberRole.CO_HOST : MemberRole.PLAYER);
    }

    public void updateSettings(UUID actorMemberId, int cardsPerPlayer, boolean allowLateJoin,
                               boolean hideParticipantNames) {
        requireHost(actorMemberId);
        if (cardsPerPlayer < 1 || cardsPerPlayer > 4) {
            throw new RoomDomainException("INVALID_CARDS_PER_PLAYER", "Los cartones por jugador deben estar entre 1 y 4.");
        }
        if (status != RoomStatus.WAITING && this.cardsPerPlayer != cardsPerPlayer) {
            throw new RoomDomainException("CARDS_SETTING_LOCKED", "Los cartones por jugador sólo pueden cambiarse antes de iniciar.");
        }
        this.cardsPerPlayer = cardsPerPlayer;
        this.allowLateJoin = allowLateJoin;
        this.hideParticipantNames = hideParticipantNames;
    }

    private void requireHost(UUID actorMemberId) {
        boolean isHost = members.stream().anyMatch(member -> member.getId().equals(actorMemberId)
                && member.getRole() == MemberRole.HOST);
        if (!isHost) throw new RoomDomainException("HOST_REQUIRED", "Sólo el host puede realizar esta acción.");
    }

    private void requireController(UUID actorMemberId) {
        boolean isController = members.stream().anyMatch(member -> member.getId().equals(actorMemberId)
                && (member.getRole() == MemberRole.HOST || member.getRole() == MemberRole.CO_HOST));
        if (!isController) throw new RoomDomainException("CONTROLLER_REQUIRED", "Se requieren permisos de anfitrión.");
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public RoomStatus getStatus() { return status; }
    public boolean isLocked() { return locked; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getCardsPerPlayer() { return cardsPerPlayer; }
    public boolean isAllowLateJoin() { return allowLateJoin; }
    public boolean isHideParticipantNames() { return hideParticipantNames; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    public List<RoomMember> getMembers() { return Collections.unmodifiableList(members); }
}
