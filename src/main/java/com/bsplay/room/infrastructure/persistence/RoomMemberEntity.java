package com.bsplay.room.infrastructure.persistence;

import com.bsplay.room.domain.model.ConnectionStatus;
import com.bsplay.room.domain.model.MemberRole;
import com.bsplay.room.domain.model.RoomMember;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_members")
class RoomMemberEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;
    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private MemberRole role;
    @Enumerated(EnumType.STRING) @Column(name = "connection_status", nullable = false, length = 20)
    private ConnectionStatus connectionStatus;
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected RoomMemberEntity() {}

    RoomMemberEntity(RoomEntity room, RoomMember member) {
        this.room = room;
        sync(member);
    }

    void sync(RoomMember member) {
        this.id = member.getId();
        this.displayName = member.getDisplayName();
        this.role = member.getRole();
        this.connectionStatus = member.getConnectionStatus();
        this.joinedAt = member.getJoinedAt();
    }

    UUID getId() { return id; }

    RoomMember toDomain() {
        return new RoomMember(id, displayName, role, connectionStatus, joinedAt);
    }
}
