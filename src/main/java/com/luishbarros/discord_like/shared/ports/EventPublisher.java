package com.luishbarros.discord_like.shared.ports;

public interface EventPublisher {
    void publish(Object event);
}
