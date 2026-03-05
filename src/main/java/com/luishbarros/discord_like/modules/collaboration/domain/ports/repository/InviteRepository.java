package com.luishbarros.discord_like.modules.collaboration.domain.ports.repository;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Invite;

import java.util.Optional;

public interface InviteRepository {
    Invite save(Invite invite);

    Optional<Invite> findByCode(String inviteCodeValue);
    Optional<Invite> findById(Long inviteId);

    void delete(Invite invite);
}
