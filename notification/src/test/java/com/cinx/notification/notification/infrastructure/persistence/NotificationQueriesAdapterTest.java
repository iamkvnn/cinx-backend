package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.notification.notification.application.query.TypeCount;
import com.cinx.notification.notification.application.query.UserNotificationView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueriesAdapterTest {
    @Mock
    private UserNotificationJpaRepository jpa;
    private NotificationQueriesAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new NotificationQueriesAdapter(jpa, Mappers.getMapper(UserNotificationPersistenceMapper.class));
    }

    @Test
    void findPageKeepsThePageAndTurnsEachRowIntoAView() {
        NotificationJpaEntity content = NotificationJpaEntity.builder().id("n-1").title("Title").message("Message").type("T").build();
        UserNotificationJpaEntity row = UserNotificationJpaEntity.builder().id("row-1").userId("user-1").notificationId("n-1")
                .isRead(false).sentAt(LocalDateTime.of(2025, 1, 1, 10, 0)).notification(content).build();
        // page 2 of 5, one-based, is zero-based page 1; with no sort named, newest first
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "sentAt"));
        when(jpa.findByUserId("q", "user-1", false, "T", pageable)).thenReturn(new PageImpl<>(List.of(row), pageable, 11));

        PageResult<UserNotificationView> page = adapter.findPage("user-1", "q", false, "T", new PageQuery(2, 5, null));

        assertThat(page.totalElements()).isEqualTo(11);
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.content()).singleElement().satisfies(view -> {
            assertThat(view.id()).isEqualTo("row-1");
            assertThat(view.title()).isEqualTo("Title");
            assertThat(view.read()).isFalse();
        });
    }

    @Test
    void findPageHonoursASortTheCallerNamesInsteadOfTheDefault() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "sentAt"));
        when(jpa.findByUserId(null, "user-1", null, null, pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResult<UserNotificationView> page = adapter.findPage("user-1", null, null, null, new PageQuery(1, 10, "{\"sentAt\":\"ASC\"}"));

        assertThat(page.content()).isEmpty();
        verify(jpa).findByUserId(null, "user-1", null, null, pageable);
    }

    @Test
    void countUnreadTurnsANullCountIntoZero() {
        when(jpa.countByUserIdAndReadFalse("user-1")).thenReturn(null);
        when(jpa.countByUserIdAndReadFalse("user-2")).thenReturn(4L);

        assertThat(adapter.countUnread("user-1")).isZero();
        assertThat(adapter.countUnread("user-2")).isEqualTo(4);
    }

    @Test
    void countsByTypeReadsTheTypeTheTotalAndTheUnreadSum() {
        // the unread SUM is null when a type has no rows to add up
        when(jpa.countByTypeForUser("user-1")).thenReturn(List.of(
                new Object[]{"COURSE_PUBLISHED", 5L, 3L},
                new Object[]{"PAYMENT_PAID", 2L, null},
                new Object[]{null, 1L, 1L}));

        assertThat(adapter.countsByType("user-1")).containsExactly(
                new TypeCount("COURSE_PUBLISHED", 5, 3),
                new TypeCount("PAYMENT_PAID", 2, 0),
                new TypeCount(null, 1, 1));
    }
}
