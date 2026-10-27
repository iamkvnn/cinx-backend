package com.cinx.learning.engagement.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Port: where UserStreak rows are stored. */
public interface UserStreakRepository {
    Optional<UserStreak> findByUserId(String userId);

    /** Learners whose streak of at least {@code minStreak} days was last extended on {@code lastActivityDate}. */
    List<String> findUserIdsWithStreakLastExtendedOn(int minStreak, LocalDate lastActivityDate);

    Optional<UserStreak> findForUpdateByUserId(String userId);

    UserStreak save(UserStreak entity);
}
