package com.luishbarros.discord_like.modules.auth.infrastructure.event;

import com.luishbarros.discord_like.modules.auth.domain.event.UserEvents;
import com.luishbarros.discord_like.shared.ports.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener implements EventListener<UserEvents> {

    @Override
    @KafkaListener(topics = "user-events", groupId = "discord-like")
    public void onEvent(UserEvents event) {
        switch (event.type()) {
            case REGISTERED       -> onRegistered(event);
            case PASSWORD_CHANGED -> onPasswordChanged(event);
            case DEACTIVATED      -> onDeactivated(event);
            case ACTIVATED        -> onActivated(event);
        }
    }

    private void onRegistered(UserEvents event) {
        // futuro: enviar email de boas-vindas
    }

    private void onPasswordChanged(UserEvents event) {
        // futuro: enviar email de confirmação de troca de senha
    }

    private void onDeactivated(UserEvents event) {
        // futuro: invalidar sessões WebSocket do usuário
    }

    private void onActivated(UserEvents event) {
        // futuro: notificar usuário que conta foi reativada
    }
}