package com.luishbarros.discord_like.modules.collaboration.application.service;

import com.luishbarros.discord_like.modules.collaboration.application.factory.InviteFactory;
import com.luishbarros.discord_like.modules.collaboration.domain.error.ForbiddenError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.InvalidInviteCodeError;
import com.luishbarros.discord_like.modules.collaboration.domain.error.RoomNotFoundError;
import com.luishbarros.discord_like.modules.collaboration.domain.event.InviteEvents;
import com.luishbarros.discord_like.modules.collaboration.domain.event.RoomEvents;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Invite;
import com.luishbarros.discord_like.modules.collaboration.domain.model.Room;
import com.luishbarros.discord_like.modules.collaboration.domain.model.RoomMembership;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.InviteRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.ports.repository.RoomMembershipRepository;
import com.luishbarros.discord_like.modules.collaboration.domain.service.RoomMembershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class InviteService {

    private final InviteFactory inviteFactory;
    private final InviteRepository inviteRepository;
    private final RoomRepository roomRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final RoomMembershipValidator membershipValidator;
    private final EventPublisher eventPublisher;

    public InviteService(
            InviteFactory inviteFactory,
            InviteRepository inviteRepository,
            RoomRepository roomRepository,
            RoomMembershipRepository roomMembershipRepository,
            RoomMembershipValidator membershipValidator,
            EventPublisher eventPublisher
    ) {
        this.inviteFactory = inviteFactory;
        this.inviteRepository = inviteRepository;
        this.roomRepository = roomRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.membershipValidator = membershipValidator;
        this.eventPublisher = eventPublisher;
    }

    public Invite generateInvite(Long roomId, Long userId, Instant now) {
        membershipValidator.validateAndGetRoom(roomId, userId);

        Invite invite = inviteFactory.create(roomId, userId, now);
        Invite saved = inviteRepository.save(invite);

        eventPublisher.publish(InviteEvents.created(saved));

        return saved;
    }

    public void acceptInvite(String inviteCodeValue, Long userId, Instant now) {
        Invite invite = inviteRepository.findByCode(inviteCodeValue)
                .orElseThrow(() -> new InvalidInviteCodeError("Invite code not found"));

        if (invite.isExpired(now)) {
            throw new InvalidInviteCodeError("Invite has expired");
        }

        Room room = roomRepository.findById(invite.getRoomId())
                .orElseThrow(() -> new RoomNotFoundError(invite.getRoomId().toString()));

        if (!roomMembershipRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
            RoomMembership newMembership = RoomMembership.create(room.getId(), userId, now);
            roomMembershipRepository.save(newMembership);
        }

        eventPublisher.publish(RoomEvents.memberJoined(room.getId(), userId, now));
        eventPublisher.publish(InviteEvents.accepted(invite, userId));
    }

    public void revokeInvite(Long roomId, Long userId, Long inviteId) {
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

    private void validateOwnership(Room room, Long userId) {
        if (!room.isOwner(userId)) {
            throw new ForbiddenError("Only room owner can revoke invites");
        }
    }
}
