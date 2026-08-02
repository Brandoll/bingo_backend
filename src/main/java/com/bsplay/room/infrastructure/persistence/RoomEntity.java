package com.bsplay.room.infrastructure.persistence;

import com.bsplay.room.domain.model.Room;
import com.bsplay.room.domain.model.RoomStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Table(name = "rooms")
class RoomEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 6)
    private String code;
    @Column(nullable = false, length = 80)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RoomStatus status;
    @Column(name = "is_locked", nullable = false)
    private boolean locked;
    @Column(name = "max_players", nullable = false)
    private int maxPlayers;
    @Column(name = "cards_per_player", nullable = false)
    private int cardsPerPlayer;
    @Column(name = "allow_late_join", nullable = false)
    private boolean allowLateJoin;
    @Column(name = "hide_participant_names", nullable = false)
    private boolean hideParticipantNames;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Version @Column(nullable = false)
    private long version;
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("joinedAt ASC")
    private List<RoomMemberEntity> members = new ArrayList<>();

    protected RoomEntity() {}

    static RoomEntity from(Room room) {
        var entity = new RoomEntity();
        entity.id = room.getId();
        entity.sync(room);
        return entity;
    }

    void sync(Room room) {
        this.code = room.getCode();
        this.name = room.getName();
        this.status = room.getStatus();
        this.locked = room.isLocked();
        this.maxPlayers = room.getMaxPlayers();
        this.cardsPerPlayer = room.getCardsPerPlayer();
        this.allowLateJoin = room.isAllowLateJoin();
        this.hideParticipantNames = room.isHideParticipantNames();
        this.createdAt = room.getCreatedAt();
        var byId = members.stream().collect(Collectors.toMap(RoomMemberEntity::getId, Function.identity()));
        var activeIds = room.getMembers().stream().map(com.bsplay.room.domain.model.RoomMember::getId).collect(Collectors.toSet());
        members.removeIf(member -> !activeIds.contains(member.getId()));
        room.getMembers().forEach(member -> {
            var memberEntity = byId.get(member.getId());
            if (memberEntity == null) members.add(new RoomMemberEntity(this, member));
            else memberEntity.sync(member);
        });
    }

    Room toDomain() {
        return new Room(id, code, name, status, locked, maxPlayers, cardsPerPlayer,
                allowLateJoin, hideParticipantNames, createdAt, version,
                members.stream().map(RoomMemberEntity::toDomain).toList());
    }
}
