package com.bsplay.room.domain.model;

import com.bsplay.room.domain.exception.RoomDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomTest {
    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

    @Test
    void createsWaitingRoomWithHost() {
        Room room = Room.create("ABC234", "Bingo familiar", "Sofía", 20, NOW);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.getMembers()).singleElement().satisfies(host -> {
            assertThat(host.getDisplayName()).isEqualTo("Sofía");
            assertThat(host.getRole()).isEqualTo(MemberRole.HOST);
        });
    }

    @Test
    void joinsPlayerAndRejectsRepeatedName() {
        Room room = Room.create("ABC234", "Bingo familiar", "Sofía", 20, NOW);

        room.join("Mateo", NOW.plusSeconds(10));

        assertThat(room.getMembers()).hasSize(2);
        assertThatThrownBy(() -> room.join("mateo", NOW.plusSeconds(20)))
                .isInstanceOf(RoomDomainException.class)
                .extracting("code").isEqualTo("DISPLAY_NAME_TAKEN");
    }

    @Test
    void rejectsPlayersWhenCapacityIsReached() {
        Room room = Room.create("ABC234", "Bingo familiar", "Sofía", 2, NOW);
        room.join("Mateo", NOW.plusSeconds(10));

        assertThatThrownBy(() -> room.join("Luna", NOW.plusSeconds(20)))
                .isInstanceOf(RoomDomainException.class)
                .extracting("code").isEqualTo("ROOM_FULL");
    }

    @Test
    void onlyHostCanStartWaitingRoom() {
        Room room = Room.create("ABC234", "Bingo familiar", "Sofía", 20, NOW);
        RoomMember player = room.join("Mateo", NOW.plusSeconds(10));

        assertThatThrownBy(() -> room.start(player.getId()))
                .isInstanceOf(RoomDomainException.class)
                .extracting("code").isEqualTo("HOST_REQUIRED");

        room.start(room.getMembers().getFirst().getId());
        assertThat(room.getStatus()).isEqualTo(RoomStatus.RUNNING);
    }

    @Test
    void hostConfiguresCardsAndDelegatesControl() {
        Room room = Room.create("ABC234", "Bingo familiar", "Sofía", 20, NOW);
        RoomMember player = room.join("Mateo", NOW.plusSeconds(10));
        var host = room.getMembers().getFirst();

        room.updateSettings(host.getId(), 3, false, true);
        room.setCoHost(host.getId(), player.getId(), true);

        assertThat(room.getCardsPerPlayer()).isEqualTo(3);
        assertThat(room.isAllowLateJoin()).isFalse();
        assertThat(room.isHideParticipantNames()).isTrue();
        assertThat(player.getRole()).isEqualTo(MemberRole.CO_HOST);
    }
}
