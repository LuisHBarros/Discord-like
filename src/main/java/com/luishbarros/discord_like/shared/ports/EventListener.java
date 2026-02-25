package com.luishbarros.discord_like.shared.ports;

public interface EventListener<T> {
    void onEvent(T event);
}
