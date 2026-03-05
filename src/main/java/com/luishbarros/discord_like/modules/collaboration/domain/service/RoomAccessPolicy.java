package com.luishbarros.discord_like.modules.collaboration.domain.service;

public interface RoomAccessPolicy {
    boolean canJoinRoom(Long roomId, Long userId, String inviteCode);
    boolean canLeaveRoom(Long roomId, Long userId, boolean isOwner);
    boolean canDeleteRoom(Long roomId, Long userId, boolean isOwner);
}
