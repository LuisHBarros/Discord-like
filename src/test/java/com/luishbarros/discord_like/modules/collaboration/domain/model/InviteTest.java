package com.luishbarros.discord_like.modules.collaboration.domain.model;

import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.InviteCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InviteTest {

    private static final Long INVITE_ID = 1L;
    private static final Long ROOM_ID = 10L;
    private static final Long CREATED_BY_USER_ID = 20L;
    private static final String CODE_VALUE = "ABCD1234";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void constructor_createsInviteWith24HourExpiration() {
        Invite invite = new Invite(ROOM_ID, CREATED_BY_USER_ID, new InviteCode(CODE_VALUE), NOW);

        assertThat(invite.getId()).isNull(); // ID is null until persisted
        assertThat(invite.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(invite.getCreatedByUserId()).isEqualTo(CREATED_BY_USER_ID);
        assertThat(invite.getCode().value()).isEqualTo(CODE_VALUE);
        assertThat(invite.getCreatedAt()).isEqualTo(NOW);
        assertThat(invite.getExpiresAt()).isEqualTo(NOW.plusSeconds(3600 * 24)); // 24 hours
    }

    @Test
    void reconstitute_restoresInviteFromDatabase() {
        Instant expiresAt = NOW.plusSeconds(3600 * 24);

        Invite invite = Invite.reconstitute(
                INVITE_ID,
                ROOM_ID,
                CREATED_BY_USER_ID,
                new InviteCode(CODE_VALUE),
                NOW,
                expiresAt
        );

        assertThat(invite.getId()).isEqualTo(INVITE_ID);
        assertThat(invite.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(invite.getCreatedByUserId()).isEqualTo(CREATED_BY_USER_ID);
        assertThat(invite.getCode().value()).isEqualTo(CODE_VALUE);
        assertThat(invite.getCreatedAt()).isEqualTo(NOW);
        assertThat(invite.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void isExpired_withPastExpiration_returnsTrue() {
        Invite invite = Invite.reconstitute(
                INVITE_ID,
                ROOM_ID,
                CREATED_BY_USER_ID,
                new InviteCode(CODE_VALUE),
                NOW.minusSeconds(3600 * 48), // Created 48 hours ago
                NOW.minusSeconds(1) // Expired 1 second ago
        );

        assertThat(invite.isExpired(NOW)).isTrue();
    }

    @Test
    void isExpired_withFutureExpiration_returnsFalse() {
        Instant expiresAt = NOW.plusSeconds(3600 * 24);

        Invite invite = Invite.reconstitute(
                INVITE_ID,
                ROOM_ID,
                CREATED_BY_USER_ID,
                new InviteCode(CODE_VALUE),
                NOW,
                expiresAt
        );

        assertThat(invite.isExpired(NOW)).isFalse();
    }

    @Test
    void isExpired_atExactExpirationTime_returnsFalse() {
        Instant expiresAt = NOW.plusSeconds(3600 * 24);

        Invite invite = Invite.reconstitute(
                INVITE_ID,
                ROOM_ID,
                CREATED_BY_USER_ID,
                new InviteCode(CODE_VALUE),
                NOW,
                expiresAt
        );

        // At the exact expiration time, it's not expired (only after)
        assertThat(invite.isExpired(expiresAt)).isFalse();
    }

    @Test
    void getRoomId_returnsCorrectRoomId() {
        Invite invite = new Invite(ROOM_ID, CREATED_BY_USER_ID, new InviteCode(CODE_VALUE), NOW);

        assertThat(invite.getRoomId()).isEqualTo(ROOM_ID);
    }

    @Test
    void getCreatedByUserId_returnsCorrectCreator() {
        Invite invite = new Invite(ROOM_ID, CREATED_BY_USER_ID, new InviteCode(CODE_VALUE), NOW);

        assertThat(invite.getCreatedByUserId()).isEqualTo(CREATED_BY_USER_ID);
    }

    @Test
    void getCode_returnsCorrectCode() {
        InviteCode code = new InviteCode(CODE_VALUE);
        Invite invite = new Invite(ROOM_ID, CREATED_BY_USER_ID, code, NOW);

        assertThat(invite.getCode()).isEqualTo(code);
    }

    @Test
    void getCreatedAt_returnsCorrectCreationTime() {
        Invite invite = new Invite(ROOM_ID, CREATED_BY_USER_ID, new InviteCode(CODE_VALUE), NOW);

        assertThat(invite.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void getExpiresAt_returnsCorrectExpirationTime() {
        Invite invite = new Invite(ROOM_ID, CREATED_BY_USER_ID, new InviteCode(CODE_VALUE), NOW);
        Instant expectedExpiresAt = NOW.plusSeconds(3600 * 24);

        assertThat(invite.getExpiresAt()).isEqualTo(expectedExpiresAt);
    }
}