package com.luishbarros.discord_like.modules.collaboration.domain.service;

public interface MessageDeliveryPolicy {
    boolean canDeliverToUser(Long senderId, Long recipientId);
    boolean shouldEncrypt(boolean isPrivateRoom);
}
