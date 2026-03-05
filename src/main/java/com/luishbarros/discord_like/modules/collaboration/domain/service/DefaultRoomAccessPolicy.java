package com.luishbarros.discord_like.modules.collaboration.domain.service;

import org.springframework.stereotype.Service;

@Service
public class DefaultRoomAccessPolicy implements RoomAccessPolicy {

    @Override
    public boolean canJoinRoom(Long roomId, Long userId, String inviteCode) {
        // Join is allowed if invite code is valid (handled by service)
        return inviteCode != null && !inviteCode.isBlank();
    }

    @Override
    public boolean canLeaveRoom(Long roomId, Long userId, boolean isOwner) {
        // Owner cannot leave room - must transfer ownership first
        return !isOwner;
    }

    @Override
    public boolean canDeleteRoom(Long roomId, Long userId, boolean isOwner) {
        // Only owner can delete room
        return isOwner;
    }
}
