package com.luishbarros.discord_like.shared.adapters.presence;

import com.luishbarros.discord_like.shared.ports.RateLimiter;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryRateLimiter implements RateLimiter {

    private final Map<String, WindowState> state = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key, int maxRequests, long windowInSeconds) {
        WindowState windowState = state.computeIfAbsent(key, k -> new WindowState(windowInSeconds));
        return windowState.tryAcquire(maxRequests);
    }

    public void clear() {
        state.clear();
    }

    private class WindowState {
        private final Queue<Instant> requests = new ConcurrentLinkedQueue<>();
        private final Object lock = new Object();
        private final long windowDurationMillis;

        WindowState(long windowInSeconds) {
            this.windowDurationMillis = windowInSeconds * 1000;
        }

        boolean tryAcquire(int maxRequests) {
            synchronized (lock) {
                Instant now = Instant.now();
                Instant windowStart = now.minusMillis(windowDurationMillis);

                // Remove old requests outside window
                while (!requests.isEmpty() && requests.peek().isBefore(windowStart)) {
                    requests.poll();
                }

                // Check limit
                if (requests.size() >= maxRequests) {
                    return false;
                }

                requests.add(now);
                return true;
            }
        }
    }
}
