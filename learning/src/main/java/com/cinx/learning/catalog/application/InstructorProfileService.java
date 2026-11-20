package com.cinx.learning.catalog.application;

import com.cinx.learning.shared.application.UpstreamCallException;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.query.InstructorPublicView;
import com.cinx.learning.catalog.application.query.UserView;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.CourseRepository.InstructorPublishedStats;
import com.cinx.learning.catalog.application.InstructorDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstructorProfileService implements IInstructorProfileService {
    private final CourseRepository courseRepository;
    private final InstructorDirectory userService;

    /** No transaction: the user-service call must not hold a database connection; each query is its own read. */
    @Override
    public InstructorPublicView getPublicProfile(String instructorId) {
        UserView user = visibleInstructor(instructorId);
        InstructorPublishedStats stats = courseRepository.summarizePublishedByInstructor(instructorId);
        List<InstructorPublicView.Category> categories = courseRepository.countPublishedByCategoryForInstructor(instructorId).stream()
                .map(row -> new InstructorPublicView.Category((String) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();
        return new InstructorPublicView(
                instructorId,
                user.name(),
                user.avatarUrl(),
                user.bio(),
                user.createdAt() == null ? null : user.createdAt().toLocalDate(),
                value(stats.getCourseCount()),
                value(stats.getEnrollmentCount()),
                weightedAverage(stats),
                value(stats.getRatingCount()),
                categories);
    }

    /** The same answer for an unknown id, a non-instructor, an unverified and a terminated partner. */
    private UserView visibleInstructor(String instructorId) {
        try {
            ApiResponse<Boolean> verified = userService.isInstructorVerified(instructorId);
            if (verified == null || !Boolean.TRUE.equals(verified.data())) {
                throw notFound();
            }
            ApiResponse<UserView> profile = userService.getInstructorById(instructorId);
            if (profile == null || profile.data() == null) {
                throw notFound();
            }
            return profile.data();
        } catch (UpstreamCallException e) {
            if (!e.isNotFound()) {
                throw e;
            }
            throw notFound();
        }
    }

    private static Double weightedAverage(InstructorPublishedStats stats) {
        long rated = value(stats.getRatedCount());
        if (rated == 0 || stats.getWeightedRatingSum() == null) {
            return null;
        }
        return BigDecimal.valueOf(stats.getWeightedRatingSum())
                .divide(BigDecimal.valueOf(rated), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static long value(Long number) {
        return number == null ? 0L : number;
    }

    private static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.INSTRUCTOR_NOT_FOUND, "Instructor not found");
    }
}
