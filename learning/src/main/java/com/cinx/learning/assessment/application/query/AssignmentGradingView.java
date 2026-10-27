package com.cinx.learning.assessment.application.query;

import java.time.Instant;
import java.util.List;

/** Everything the inbox grader needs for one assignment: its criteria and every submission, ungraded first. */
public record AssignmentGradingView(
        String lessonId,
        String courseId,
        String title,
        String description,
        List<Criterion> criteria,
        List<Submission> submissions
) {
    public record Criterion(String id, String title, String description, Double maxPoints, Integer orderIndex) {
    }

    public record Submission(
            String id,
            String userId,
            String learnerName,
            String learnerAvatarUrl,
            String content,
            Instant submissionTime,
            int attempt,
            Long version,
            List<Attachment> attachments,
            Double score,
            String feedback,
            Instant gradedAt,
            String gradedBy,
            boolean aiAssisted,
            List<CriterionScore> criterionScores,
            List<Adjustment> adjustments,
            List<LineComment> comments
    ) {
    }

    public record Attachment(String id, String fileName, String fileType, Long fileSize, String attachmentUrl, boolean textReadable) {
    }

    public record CriterionScore(String criterionId, Double points) {
    }

    public record Adjustment(String id, Double points, String reason) {
    }

    public record LineComment(String id, String attachmentId, Integer line, String content, String authorId, Instant createdAt) {
    }

    public record AttachmentText(String content) {
    }
}
