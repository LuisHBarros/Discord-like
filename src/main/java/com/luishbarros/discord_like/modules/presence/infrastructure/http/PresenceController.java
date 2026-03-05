package com.luishbarros.discord_like.modules.presence.infrastructure.http;

import com.luishbarros.discord_like.modules.presence.application.dto.PresenceStatus;
import com.luishbarros.discord_like.modules.presence.application.ports.in.QueryPresenceUseCase;
import com.luishbarros.discord_like.modules.presence.application.ports.in.TrackPresenceUseCase;
import com.luishbarros.discord_like.modules.presence.domain.model.value_object.PresenceState;

import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/presence")
public class PresenceController {

    private final TrackPresenceUseCase trackPresenceUseCase;
    private final QueryPresenceUseCase queryPresenceUseCase;

    public PresenceController(TrackPresenceUseCase trackPresenceUseCase,
                              QueryPresenceUseCase queryPresenceUseCase) {
        this.trackPresenceUseCase = trackPresenceUseCase;
        this.queryPresenceUseCase = queryPresenceUseCase;
    }

    @GetMapping("/{userId}")
    public PresenceStatus getPresenceStatus(@PathVariable Long userId) {
        return queryPresenceUseCase.getPresenceStatus(userId);
    }

    @GetMapping("/online")
    public Set<PresenceStatus> getOnlineUsers() {
        return queryPresenceUseCase.getOnlineUsers();
    }

    @GetMapping("/online/ids")
    public Set<Long> getOnlineUserIds() {
        return queryPresenceUseCase.getOnlineUserIds();
    }

    @PostMapping("/{userId}/online")
    public void setOnline(@PathVariable Long userId) {
        trackPresenceUseCase.setOnline(userId);
    }

    @PostMapping("/{userId}/offline")
    public void setOffline(@PathVariable Long userId) {
        trackPresenceUseCase.setOffline(userId);
    }

    @PostMapping("/{userId}/state")
    public void setPresenceState(@PathVariable Long userId,
                                 @RequestParam String state) {
        PresenceState presenceState = PresenceState.fromString(state);
        trackPresenceUseCase.setPresenceState(userId, presenceState);
    }

    @PostMapping("/{userId}/activity")
    public void updateLastActivity(@PathVariable Long userId) {
        trackPresenceUseCase.updateLastActivity(userId);
    }
}
