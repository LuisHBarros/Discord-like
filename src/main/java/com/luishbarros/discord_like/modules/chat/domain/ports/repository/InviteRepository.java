package com.luishbarros.discord_like.modules.chat.domain.ports.repository;

import com.luishbarros.discord_like.modules.chat.domain.model.Invite;

import java.util.Optional;
import java.util.UUID;

public interface InviteRepository {
    void save(Invite invite);

    Optional<Invite> findByCode(String inviteCodeValue);
    Optional<Invite> findById(UUID inviteId);

    void delete(Invite invite);
}
