package com.cinx.learning.community.application.query;

import java.time.Instant;
import java.util.List;

/** What the inbox questions tool reads and gets back. */
public final class InstructorQnAResponses {
    private InstructorQnAResponses() {
    }

    public record Question(
            String id,
            String courseId,
            String lessonId,
            String lessonTitle,
            String userId,
            String learnerName,
            String learnerAvatarUrl,
            String title,
            String content,
            int upvoteCount,
            Instant createdAt,
            boolean answered
    ) {
    }

    public record PastAnswer(String answerId, String questionId, String questionTitle, String content, Instant createdAt, long helpfulCount) {
    }

    public record BatchResult(List<Result> results) {
    }

    public record Result(String questionId, String answerId, boolean ok, String error) {
    }
}
