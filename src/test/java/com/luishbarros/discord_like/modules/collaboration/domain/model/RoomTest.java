package com.luishbarros.discord_like.modules.collaboration.domain.model;

import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidRoomError;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.RoomName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomTest {

    private static final Long ROOM_ID = 1L;
    private static final Long OWNER_ID = 10L;
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void constructor_createsRoom() {
        Room room = new Room(new RoomName("General"), OWNER_ID, NOW);

        assertThat(room.getId()).isNull();
        assertThat(room.getName().value()).isEqualTo("General");
        assertThat(room.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(room.getCreatedAt()).isEqualTo(NOW);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void reconstitute_restoresRoomFromDatabase() {
        Room room = Room.reconstitute(
                ROOM_ID,
                new RoomName("General"),
                OWNER_ID,
                NOW,
                NOW
        );

        assertThat(room.getId()).isEqualTo(ROOM_ID);
        assertThat(room.getName().value()).isEqualTo("General");
        assertThat(room.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(room.getCreatedAt()).isEqualTo(NOW);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void setName_updatesRoomNameAndTimestamp() {
        Room room = new Room(new RoomName("General"), OWNER_ID, NOW);
        Instant later = NOW.plusSeconds(60);

        room.setName(new RoomName("Updated Name"), later);

        assertThat(room.getName().value()).isEqualTo("Updated Name");
        assertThat(room.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void isOwner_withCorrectOwner_returnsTrue() {
        Room room = new Room(new RoomName("General"), OWNER_ID, NOW);

        assertThat(room.isOwner(OWNER_ID)).isTrue();
    }

    @Test
    void isOwner_withDifferentUser_returnsFalse() {
        Room room = new Room(new RoomName("General"), OWNER_ID, NOW);
        Long differentUserId = 20L;

        assertThat(room.isOwner(differentUserId)).isFalse();
    }

    @Test
    void setName_emptyName_throwsException() {
        Room room = new Room(new RoomName("General"), OWNER_ID, NOW);

        assertThatThrownBy(() -> room.setName(new RoomName(""), NOW))
                .isInstanceOf(InvalidRoomError.class);
    }

    @Test
    void setName_nullName_updatesNameToNull() {
        Room room = new Room(new RoomName("General"), OWNER_ID, NOW);

        room.setName(null, NOW);

        assertThat(room.getName()).isNull();
    }
}