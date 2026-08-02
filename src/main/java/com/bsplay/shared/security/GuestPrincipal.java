package com.bsplay.shared.security;

import com.bsplay.room.domain.model.MemberRole;

import java.util.UUID;

public record GuestPrincipal(UUID memberId, UUID roomId, String displayName, MemberRole role) {}
