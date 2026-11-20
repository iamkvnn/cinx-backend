package com.cinx.learning.instructor.application.query;

import com.cinx.learning.shared.domain.InboxKind;

import java.time.Instant;
import java.util.List;

/** The instructor inbox: work grouped by course and kind, with each source's own status. */
public record InboxView(
        Sources sources,
        List<CourseWork> courses,
        List<Group> groups,
        long doneToday
) {
    public enum SourceStatus { OK, FAILED }

    public enum Urgency { OVERDUE, DUE_SOON, NEW }

    public record Sources(
            SourceStatus grading,
            SourceStatus questions,
            SourceStatus reviews,
            SourceStatus certificates,
            SourceStatus quizEssays,
            SourceStatus courseRejected
    ) {
    }

    public record CourseWork(String courseId, String title, String thumbnailUrl, long waiting) {
    }

    /** title is the target's own title (a lesson, or the course for reviews, certificates and rejections). */
    public record Group(
            String key,
            InboxKind kind,
            String courseId,
            String courseTitle,
            String targetId,
            String title,
            long count,
            long upvotes,
            long eligible,
            Instant oldestAt,
            Urgency urgency,
            int estimateMinutes,
            long progressDone
    ) {
    }

    public record Summary(long waiting, long overdue, long pendingCertificates, long courses) {
    }
}
