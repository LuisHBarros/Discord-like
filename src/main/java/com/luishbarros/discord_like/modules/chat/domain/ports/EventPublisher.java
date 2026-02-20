package com.luishbarros.discord_like.modules.chat.domain.ports;

public interface EventPublisher {
    void publish(Object event);
}
