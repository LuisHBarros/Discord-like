package com.luishbarros.discord_like.modules.chat.domain.ports.repository;

import com.luishbarros.discord_like.modules.chat.domain.event.InviteEvents;
import com.luishbarros.discord_like.modules.chat.domain.event.MessageEvents;
import com.luishbarros.discord_like.modules.chat.domain.event.RoomEvents;

public interface EventListener {

    default void onMessageEvent(MessageEvents event) {
    }

    default void onRoomEvent(RoomEvents event) {
    }

    default void onInviteEvent(InviteEvents event) {
    }
}
