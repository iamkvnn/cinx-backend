package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.common.mapper.SortConverter;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.notification.notification.application.query.NotificationQueries;
import com.cinx.notification.notification.application.query.TypeCount;
import com.cinx.notification.notification.application.query.UserNotificationView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class NotificationQueriesAdapter implements NotificationQueries {
    private final UserNotificationJpaRepository jpa;
    private final UserNotificationPersistenceMapper mapper;

    @Override
    public PageResult<UserNotificationView> findPage(String userId, String query, Boolean isRead, String type, PageQuery page) {
        Sort sort = page.sort() == null || page.sort().isBlank() ? Sort.by(Sort.Direction.DESC, "sentAt") : SortConverter.toSort(page.sort());
        Page<UserNotificationJpaEntity> rows = jpa.findByUserId(query, userId, isRead, type, PageRequest.of(page.page() - 1, page.size(), sort));
        return new PageResult<>(rows.getContent().stream().map(mapper::toView).toList(), page.page(), page.size(), rows.getTotalElements());
    }

    @Override
    public long countUnread(String userId) {
        Long unread = jpa.countByUserIdAndReadFalse(userId);
        return unread == null ? 0 : unread;
    }

    @Override
    public List<TypeCount> countsByType(String userId) {
        return jpa.countByTypeForUser(userId).stream()
                .map(row -> new TypeCount((String) row[0], ((Number) row[1]).longValue(), row[2] == null ? 0 : ((Number) row[2]).longValue()))
                .toList();
    }
}
