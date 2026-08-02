package com.bsplay.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bsplay.room.application.port.GuestSessionTokenPort;
import com.bsplay.room.domain.model.MemberRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class JwtGuestTokenService implements GuestSessionTokenPort {
    private final Algorithm algorithm;
    private final Clock clock;
    private final Duration lifetime;

    public JwtGuestTokenService(@Value("${bsplay.security.jwt-secret}") String secret,
                                @Value("${bsplay.security.token-hours:8}") long tokenHours,
                                Clock clock) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.clock = clock;
        this.lifetime = Duration.ofHours(tokenHours);
    }

    @Override
    public String issue(UUID memberId, UUID roomId, String displayName, MemberRole role) {
        Instant now = clock.instant();
        return JWT.create().withIssuer("bsplay").withSubject(memberId.toString())
                .withClaim("roomId", roomId.toString()).withClaim("displayName", displayName)
                .withClaim("role", role.name()).withIssuedAt(now).withExpiresAt(now.plus(lifetime))
                .sign(algorithm);
    }

    public GuestPrincipal verify(String token) {
        var jwt = JWT.require(algorithm).withIssuer("bsplay").build().verify(token);
        return new GuestPrincipal(UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaim("roomId").asString()),
                jwt.getClaim("displayName").asString(),
                MemberRole.valueOf(jwt.getClaim("role").asString()));
    }
}
