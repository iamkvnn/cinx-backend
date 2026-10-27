package com.cinx.learning.community.application;

import com.cinx.learning.community.application.query.ReviewInfo;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** What the other features read of questions and reviews. */
public interface CommunityLookup {
    /** Questions per lesson stable id of one course; a lesson without questions is left out. */
    Map<String, Long> questionCountPerLesson(String courseId);

    /** Per lesson of these courses, the questions no instructor has answered: count, upvotes and the oldest. */
    List<UnansweredGroup> unansweredQuestionGroups(Collection<String> courseIds);

    /** Per course of these, the reviews no instructor has replied to: count and the oldest. */
    List<UnrepliedGroup> unrepliedReviewGroups(Collection<String> courseIds);

    Double averageRating(Collection<String> courseIds);

    Double averageRatingBefore(Collection<String> courseIds, LocalDateTime before);

    long reviewCount(Collection<String> courseIds);

    boolean reviewExists(String reviewId);

    List<ReviewInfo> reviews(Collection<String> reviewIds);

    @Value
    class UnansweredGroup {
        String courseId;
        String lessonId;
        long waiting;
        long upvotes;
        LocalDateTime oldest;
    }

    @Value
    class UnrepliedGroup {
        String courseId;
        long waiting;
        LocalDateTime oldest;
    }
}
