// shared/ports/PresenceStore.java
package com.luishbarros.discord_like.shared.ports;

import java.util.Set;

public interface PresenceStore {
    void setOnline(Long userId);
    void setOffline(Long userId);
    boolean isOnline(Long userId);
    Set<Long> getOnlineUsers();
}