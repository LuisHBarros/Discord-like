package com.luishbarros.discord_like.modules.collaboration.application.factory;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Invite;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.InviteCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class InviteFactory {

    public Invite create(Long roomId, Long createdByUserId, Instant now) {
        String codeValue = generateCode();
        InviteCode code = new InviteCode(codeValue, createdByUserId);
        return new Invite(roomId, createdByUserId, code, now);
    }

    private String generateCode() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
