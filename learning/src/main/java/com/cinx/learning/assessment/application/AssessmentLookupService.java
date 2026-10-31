package com.cinx.learning.assessment.application;

import com.cinx.learning.assessment.application.query.QuizSessionInfo;
import com.cinx.learning.assessment.application.query.QuizSessionSubmissionInfo;
import com.cinx.learning.assessment.domain.AssignmentSubmissionRepository;
import com.cinx.learning.assessment.domain.QuizSessionRepository;
import com.cinx.learning.assessment.domain.QuizSessionSubmissionRepository;
import com.cinx.learning.shared.domain.QuizSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssessmentLookupService implements AssessmentLookup {
    private final QuizSessionRepository quizSessionRepository;
    private final QuizSessionSubmissionRepository quizSessionSubmissionRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Override
    public List<String> quizLessonIdsWithStatus(String userId, Collection<String> quizLessonIds, Collection<QuizSessionStatus> statuses) {
        return quizSessionRepository.findLessonIdsWithStatus(userId, quizLessonIds, statuses);
    }

    @Override
    public List<String> submittedAssignmentIds(String userId, Collection<String> assignmentIds) {
        return assignmentSubmissionRepository.findSubmittedAssignmentIds(userId, assignmentIds);
    }

    @Override
    public List<QuizSessionInfo> quizSessionsOfCourse(String courseId, Collection<QuizSessionStatus> statuses) {
        return quizSessionRepository.findAllByCourseIdAndStatusIn(courseId, statuses).stream().map(QuizSessionInfo::of).toList();
    }

    @Override
    public List<QuizSessionSubmissionInfo> submissionsOfQuizSessions(Collection<String> quizSessionIds) {
        return quizSessionSubmissionRepository.findAllByQuizSessionIdIn(quizSessionIds).stream().map(QuizSessionSubmissionInfo::of).toList();
    }

    @Override
    public List<UngradedGroup> ungradedAssignmentGroups(Collection<String> assignmentIds) {
        return assignmentSubmissionRepository.findUngradedGroups(assignmentIds).stream()
                .map(row -> new UngradedGroup(row.getAssignmentId(), row.getWaiting(), row.getOldest()))
                .toList();
    }

    @Override
    public long gradedAssignmentSubmissionsSince(String assignmentId, LocalDateTime since) {
        return assignmentSubmissionRepository.countGradedSince(assignmentId, since);
    }

    @Override
    public List<PendingGradeGroup> pendingGradeGroups(Collection<String> courseIds) {
        return quizSessionRepository.findPendingGradeGroups(courseIds).stream()
                .map(row -> new PendingGradeGroup(row.getCourseId(), row.getQuizLessonId(), row.getWaiting(), row.getOldest()))
                .toList();
    }
}
