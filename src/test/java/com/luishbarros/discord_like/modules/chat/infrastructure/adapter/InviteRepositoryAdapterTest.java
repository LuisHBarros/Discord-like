package com.luishbarros.discord_like.modules.chat.infrastructure.adapter;

import com.luishbarros.discord_like.modules.chat.domain.model.Invite;
import com.luishbarros.discord_like.modules.chat.domain.model.value_object.InviteCode;
import com.luishbarros.discord_like.modules.chat.infrastructure.persistence.entity.InviteJpaEntity;
import com.luishbarros.discord_like.modules.chat.infrastructure.persistence.repository.InviteJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteRepositoryAdapterTest {

    @Mock
    private InviteJpaRepository jpaRepository;

    @InjectMocks
    private InviteRepositoryAdapter adapter;

    @Test
    void save_returnsPersistedDomainInviteWithGeneratedId() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        InviteCode code = new InviteCode("ABCD1234", 1L);
        Invite unsavedInvite = new Invite(10L, 1L, code, now);

        InviteJpaEntity persistedEntity = new InviteJpaEntity(
                50L,
                10L,
                1L,
                "ABCD1234",
                now,
                now.plusSeconds(3600 * 24)
        );

        when(jpaRepository.save(any(InviteJpaEntity.class))).thenReturn(persistedEntity);

        Invite savedInvite = adapter.save(unsavedInvite);

        ArgumentCaptor<InviteJpaEntity> entityCaptor = ArgumentCaptor.forClass(InviteJpaEntity.class);
        verify(jpaRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getId()).isNull();

        assertThat(savedInvite.getId()).isEqualTo(50L);
        assertThat(savedInvite.getRoomId()).isEqualTo(10L);
        assertThat(savedInvite.getCreatedByUserId()).isEqualTo(1L);
        assertThat(savedInvite.getCode().value()).isEqualTo("ABCD1234");
        assertThat(savedInvite.getCreatedAt()).isEqualTo(now);
    }
}
