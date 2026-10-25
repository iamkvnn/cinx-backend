package com.cinx.user.profile.infrastructure.persistence;

import com.cinx.common.mapper.SortConverter;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.user.profile.application.query.UserQueries;
import com.cinx.user.profile.application.query.UserView;
import com.cinx.user.profile.domain.InstructorApplicationStatus;
import com.cinx.user.profile.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class UserQueriesAdapter implements UserQueries {
    private final UserJpaRepository jpa;
    private final UserPersistenceMapper mapper;

    @Override
    public PageResult<UserView> findAll(String query, Boolean isInstructorVerified,
                                        InstructorApplicationStatus applicationStatus, PageQuery page) {
        Page<UserJpaEntity> rows = jpa.findAll(query, isInstructorVerified, applicationStatus,
                PageRequest.of(page.page() - 1, page.size(), SortConverter.toSort(page.sort())));
        return new PageResult<>(rows.getContent().stream().map(row -> UserView.of(toDomain(row))).toList(),
                page.page(), page.size(), rows.getTotalElements());
    }

    @Override
    public long countTotalUsers() {
        return jpa.countTotalUsers();
    }

    @Override
    public long countUsersBetween(LocalDateTime start, LocalDateTime end) {
        return jpa.countUsersBetween(start, end);
    }

    private User toDomain(UserJpaEntity entity) {
        return mapper.toDomain(entity);
    }
}
