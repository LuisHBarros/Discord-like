package com.luishbarros.discord_like.modules.presence.application.ports.in;

import com.luishbarros.discord_like.modules.presence.application.dto.PresenceStatus;

import java.util.Set;

public interface QueryPresenceUseCase {
    PresenceStatus getPresenceStatus(Long userId);
    Set<PresenceStatus> getOnlineUsers();
    Set<Long> getOnlineUserIds();
}
