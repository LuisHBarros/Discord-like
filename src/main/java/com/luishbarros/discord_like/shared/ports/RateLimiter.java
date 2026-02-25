package com.luishbarros.discord_like.shared.ports;

public interface RateLimiter {
    boolean isAllowed(String key, int maxRequests, long windowInSeconds);
}