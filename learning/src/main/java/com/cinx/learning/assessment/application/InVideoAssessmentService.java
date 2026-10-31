package com.cinx.learning.assessment.application;

import com.cinx.learning.assessment.application.query.InVideoAssessmentSubmissionView;
import com.cinx.learning.assessment.domain.InVideoAssessmentSubmission;
import com.cinx.learning.assessment.domain.InVideoAssessmentSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InVideoAssessmentService implements InVideoAssessments {
    private final InVideoAssessmentSubmissionRepository submissionRepository;

    @Override
    @Transactional
    public void recordAnswer(String userId, String videoLessonId, String videoAssessmentId, String userAnswer) {
        InVideoAssessmentSubmission submission = submissionRepository
                .findForUpdateByUserIdAndVideoAssessmentId(userId, videoAssessmentId)
                .orElseGet(() -> InVideoAssessmentSubmission.builder()
                        .userId(userId)
                        .videoLessonId(videoLessonId)
                        .videoAssessmentId(videoAssessmentId)
                        .build());
        submission.setUserAnswer(userAnswer);
        submission.setSubmissionTime(LocalDateTime.now());
        submissionRepository.save(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public long answeredCount(String userId, String videoLessonId) {
        return submissionRepository.countByUserIdAndVideoLessonId(userId, videoLessonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InVideoAssessmentSubmissionView> submissions(String userId, String videoLessonId) {
        return submissionRepository.findByUserIdAndVideoLessonId(userId, videoLessonId).stream()
                .map(sub -> new InVideoAssessmentSubmissionView(
                        sub.getVideoLessonId(),
                        sub.getVideoAssessmentId(),
                        sub.getUserAnswer(),
                        sub.getSubmissionTime()))
                .toList();
    }
}
