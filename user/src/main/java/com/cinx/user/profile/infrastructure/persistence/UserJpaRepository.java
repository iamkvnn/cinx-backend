package com.cinx.user.profile.infrastructure.persistence;

import com.cinx.user.profile.domain.InstructorApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {
    @Query("""
        SELECT u FROM UserJpaEntity u
        WHERE (:query IS NULL OR u.name LIKE %:query% OR u.email LIKE %:query%)
        AND (:isInstructorVerified IS NULL OR u.isInstructorVerified = :isInstructorVerified)
        AND (:applicationStatus IS NULL OR u.instructorApplicationStatus = :applicationStatus)
    """)
    Page<UserJpaEntity> findAll(String query, Boolean isInstructorVerified,
                                InstructorApplicationStatus applicationStatus, Pageable pageable);

    Optional<UserJpaEntity> findByEmail(String email);

    /** user.email has no unique index, so a lookup must handle several rows. */
    List<UserJpaEntity> findAllByEmailIgnoreCase(String email);

    Optional<UserJpaEntity> findByUserId(String userId);

    boolean existsByEmail(String email);

    List<UserJpaEntity> findAllByUserIdIn(List<String> ids);

    @Query("SELECT COUNT(u) FROM UserJpaEntity u")
    long countTotalUsers();

    @Query("SELECT COUNT(u) FROM UserJpaEntity u WHERE u.createdAt BETWEEN :start AND :end")
    long countUsersBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT u.isInstructorVerified, COUNT(u)
        FROM UserJpaEntity u
        WHERE u.createdAt BETWEEN :start AND :end
        GROUP BY u.isInstructorVerified
    """)
    List<Object[]> countInstructorsByVerificationStatusBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m-%d'), COUNT(u)
        FROM UserJpaEntity u
        WHERE u.createdAt BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m-%d')
        ORDER BY FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m-%d') ASC
    """)
    List<Object[]> aggregateNewUsersByDay(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m'), COUNT(u)
        FROM UserJpaEntity u
        WHERE u.createdAt BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m') ASC
    """)
    List<Object[]> aggregateNewUsersByMonth(LocalDateTime start, LocalDateTime end);
}
