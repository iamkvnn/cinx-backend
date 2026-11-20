package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.notification.notification.domain.UserNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationRepositoryAdapterTest {
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2025, 1, 1, 10, 0);

    @Mock
    private UserNotificationJpaRepository jpa;
    private UserNotificationRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserNotificationRepositoryAdapter(jpa, Mappers.getMapper(UserNotificationPersistenceMapper.class));
    }

    @Test
    void aNewNotificationIsInserted() {
        when(jpa.save(any(UserNotificationJpaEntity.class))).thenAnswer(call -> {
            UserNotificationJpaEntity entity = call.getArgument(0);
            entity.setId("row-1");
            return entity;
        });

        UserNotification saved = adapter.save(UserNotification.unread("user-1", "notification-1", SENT_AT));

        assertThat(saved.getId()).isEqualTo("row-1");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void anExistingNotificationOnlyHasItsReadFlagWrittenBack() {
        UserNotificationJpaEntity stored = UserNotificationJpaEntity.builder().id("row-1").userId("user-1")
                .notificationId("notification-1").isRead(false).sentAt(SENT_AT).build();
        when(jpa.findById("row-1")).thenReturn(Optional.of(stored));
        when(jpa.save(stored)).thenReturn(stored);
        UserNotification changed = UserNotification.builder().id("row-1").userId("someone-else").notificationId("other")
                .read(true).sentAt(SENT_AT.plusDays(1)).build();

        UserNotification saved = adapter.save(changed);

        // the flag moves; the owner, the content and the time are not the domain's to rewrite
        assertThat(stored.getIsRead()).isTrue();
        assertThat(stored.getUserId()).isEqualTo("user-1");
        assertThat(stored.getNotificationId()).isEqualTo("notification-1");
        assertThat(stored.getSentAt()).isEqualTo(SENT_AT);
        assertThat(saved.isRead()).isTrue();
    }

    @Test
    void savingARowThatVanishedFailsLoudly() {
        when(jpa.findById("row-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(UserNotification.builder().id("row-1").read(true).build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void saveAllInsertsEveryRecipientsCopy() {
        when(jpa.saveAll(any())).thenAnswer(call -> call.getArgument(0));

        List<UserNotification> saved = adapter.saveAll(List.of(
                UserNotification.unread("user-1", "notification-1", SENT_AT),
                UserNotification.unread("user-2", "notification-1", SENT_AT)));

        ArgumentCaptor<List<UserNotificationJpaEntity>> entities = ArgumentCaptor.forClass(List.class);
        verify(jpa).saveAll(entities.capture());
        assertThat(entities.getValue()).extracting(UserNotificationJpaEntity::getUserId).containsExactly("user-1", "user-2");
        assertThat(saved).hasSize(2);
    }

    @Test
    void deleteReportsWhetherARowOfThatOwnerWasRemoved() {
        when(jpa.deleteByIdAndUserId("row-1", "user-1")).thenReturn(1);
        when(jpa.deleteByIdAndUserId("row-1", "intruder")).thenReturn(0);

        assertThat(adapter.deleteByIdAndUserId("row-1", "user-1")).isTrue();
        assertThat(adapter.deleteByIdAndUserId("row-1", "intruder")).isFalse();
    }

    @Test
    void setReadForAllUpdatesInOneStatementForThatUser() {
        adapter.setReadForAll("user-1", true);

        verify(jpa).updateReadStatusByUserId("user-1", true);
    }
}
