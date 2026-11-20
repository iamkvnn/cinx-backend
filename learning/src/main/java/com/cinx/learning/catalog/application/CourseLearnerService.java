package com.cinx.learning.catalog.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.catalog.application.query.LearnerSummaryView;
import com.cinx.learning.catalog.application.query.UserView;
import com.cinx.learning.catalog.application.InstructorDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourseLearnerService implements ICourseLearnerService {
    static final int MAX_IDS = 100;

    private final ICourseAccessService courseAccessService;
    private final InstructorDirectory userService;

    @Override
    @Transactional(readOnly = true)
    public List<LearnerSummaryView> summaries(String currentUserId, String courseId, List<String> userIds) {
        if (!courseAccessService.isAdmin()
                && !courseAccessService.hasCourseCapability(currentUserId, courseId, CourseMembershipRole.MANAGER)) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "Only the course's instructors can view its learners");
        }

        LinkedHashSet<String> requested = new LinkedHashSet<>();
        if (userIds != null) {
            userIds.stream().filter(Objects::nonNull).map(String::trim).filter(id -> !id.isEmpty()).forEach(requested::add);
        }
        if (requested.size() > MAX_IDS) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "At most " + MAX_IDS + " user ids per request");
        }

        // Only people taking part in this course: an instructor cannot look up arbitrary accounts.
        List<String> participants = requested.stream()
                .filter(id -> courseAccessService.isEnrolled(id, courseId)
                        || courseAccessService.hasSubscriptionAccess(id, courseId)
                        || courseAccessService.isActiveMember(id, courseId))
                .toList();
        if (participants.isEmpty()) {
            return List.of();
        }

        ApiResponse<List<UserView>> response = userService.getInstructorsByIds(participants);
        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream()
                .filter(user -> user.userId() != null)
                .map(user -> new LearnerSummaryView(user.userId(), user.name(), user.avatarUrl()))
                .toList();
    }
}
