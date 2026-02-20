package com.luishbarros.discord_like.modules.chat.application.service;

import com.luishbarros.discord_like.modules.chat.application.factory.InviteFactory;
import com.luishbarros.discord_like.modules.chat.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.chat.domain.error.InvalidInviteCodeError;
import com.luishbarros.discord_like.modules.chat.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.chat.domain.event.InviteEvents;
import com.luishbarros.discord_like.modules.chat.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.chat.domain.model.Invite;
import com.luishbarros.discord_like.modules.chat.domain.model.Room;
import com.luishbarros.discord_like.modules.chat.domain.ports.EventPublisher;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.InviteRepository;
import com.luishbarros.discord_like.modules.chat.domain.ports.repository.RoomRepository;
import com.luishbarros.discord_like.modules.chat.domain.service.RoomMembershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class InviteService {

    private final InviteFactory inviteFactory;
    private final InviteRepository inviteRepository;
    private final RoomRepository roomRepository;
    private final RoomMembershipValidator membershipValidator;
    private final EventPublisher eventPublisher;

    public InviteService(
            InviteFactory inviteFactory,
            InviteRepository inviteRepository,
            RoomRepository roomRepository,
            RoomMembershipValidator membershipValidator,
            EventPublisher eventPublisher
    ) {
        this.inviteFactory = inviteFactory;
        this.inviteRepository = inviteRepository;
        this.roomRepository = roomRepository;
        this.membershipValidator = membershipValidator;
        this.eventPublisher = eventPublisher;
    }

    public Invite generateInvite(UUID roomId, UUID userId, Instant now) {
        membershipValidator.validateAndGetRoom(roomId, userId);

        Invite invite = inviteFactory.create(roomId, userId, now);
        inviteRepository.save(invite);

        eventPublisher.publish(InviteEvents.created(invite));

        return invite;
    }

    public void acceptInvite(String inviteCodeValue, UUID userId, Instant now) {
        Invite invite = inviteRepository.findByCode(inviteCodeValue)
                .orElseThrow(() -> new InvalidInviteCodeError("Invite code not found"));

        if (invite.isExpired(now)) {
            throw new InvalidInviteCodeError("Invite has expired");
        }

        Room room = roomRepository.findById(invite.getRoomId())
                .orElseThrow(() -> new RoomNotFoundError(invite.getRoomId().toString()));

        room.addMember(userId, now);
        roomRepository.save(room);

        eventPublisher.publish(RoomEvents.memberJoined(room.getId(), userId, now));
        eventPublisher.publish(InviteEvents.accepted(invite, userId));
    }

    public void revokeInvite(UUID roomId, UUID userId, UUID inviteId) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        validateOwnership(room, userId);

        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new InvalidInviteCodeError("Invite not found: " + inviteId));

        if (!invite.getRoomId().equals(roomId)) {
            throw new InvalidInviteCodeError("Invite does not belong to this room");
        }

        inviteRepository.delete(invite);
        eventPublisher.publish(InviteEvents.revoked(invite));
    }

    private void validateOwnership(Room room, UUID userId) {
        if (!room.isOwner(userId)) {
            throw new ForbiddenError("Only room owner can revoke invites");
        }
    }
}