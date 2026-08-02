package com.bsplay.room.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RoomMember {
    private final UUID id;
    private final String displayName;
    private MemberRole role;
    private final ConnectionStatus connectionStatus;
    private final Instant joinedAt;

    public RoomMember(UUID id, String displayName, MemberRole role,
                      ConnectionStatus connectionStatus, Instant joinedAt) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.role = Objects.requireNonNull(role);
        this.connectionStatus = Objects.requireNonNull(connectionStatus);
        this.joinedAt = Objects.requireNonNull(joinedAt);
    }

    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
    public MemberRole getRole() { return role; }
    void setRole(MemberRole role) { this.role = Objects.requireNonNull(role); }
    public ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public Instant getJoinedAt() { return joinedAt; }
}
