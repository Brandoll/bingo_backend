package com.bsplay.room.application.port;

import com.bsplay.room.domain.model.MemberRole;

import java.util.UUID;

public interface GuestSessionTokenPort {
    String issue(UUID memberId, UUID roomId, String displayName, MemberRole role);
}
