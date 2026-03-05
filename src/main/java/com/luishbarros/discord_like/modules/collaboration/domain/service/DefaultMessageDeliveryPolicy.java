package com.luishbarros.discord_like.modules.collaboration.domain.service;

import org.springframework.stereotype.Service;

@Service
public class DefaultMessageDeliveryPolicy implements MessageDeliveryPolicy {

    @Override
    public boolean canDeliverToUser(Long senderId, Long recipientId) {
        // By default, messages can be delivered within the same room
        return !senderId.equals(recipientId); // Don't send to self
    }

    @Override
    public boolean shouldEncrypt(boolean isPrivateRoom) {
        // Always encrypt messages
        return true;
    }
}
