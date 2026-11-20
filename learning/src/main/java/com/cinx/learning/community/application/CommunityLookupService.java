package com.cinx.learning.community.application;

import com.cinx.learning.community.application.query.ReviewInfo;
import com.cinx.learning.community.domain.CourseQuestionRepository;
import com.cinx.learning.community.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityLookupService implements CommunityLookup {
    private final CourseQuestionRepository questionRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public Map<String, Long> questionCountPerLesson(String courseId) {
        return questionRepository.countPerLesson(courseId).stream()
                .collect(Collectors.toMap(CourseQuestionRepository.LessonCount::getLessonId, CourseQuestionRepository.LessonCount::getTotal));
    }

    @Override
    public List<UnansweredGroup> unansweredQuestionGroups(Collection<String> courseIds) {
        return questionRepository.findUnansweredGroups(courseIds).stream()
                .map(row -> new UnansweredGroup(row.getCourseId(), row.getLessonId(), row.getWaiting(), row.getUpvotes(), row.getOldest()))
                .toList();
    }

    @Override
    public List<UnrepliedGroup> unrepliedReviewGroups(Collection<String> courseIds) {
        return reviewRepository.findUnrepliedGroups(courseIds).stream()
                .map(row -> new UnrepliedGroup(row.getCourseId(), row.getWaiting(), row.getOldest()))
                .toList();
    }

    @Override
    public Double averageRating(Collection<String> courseIds) {
        return reviewRepository.averageRating(courseIds);
    }

    @Override
    public Double averageRatingBefore(Collection<String> courseIds, LocalDateTime before) {
        return reviewRepository.averageRatingBefore(courseIds, before);
    }

    @Override
    public long reviewCount(Collection<String> courseIds) {
        return reviewRepository.countByCourseIdIn(courseIds);
    }

    @Override
    public boolean reviewExists(String reviewId) {
        return reviewRepository.existsById(reviewId);
    }

    @Override
    public List<ReviewInfo> reviews(Collection<String> reviewIds) {
        return reviewRepository.findAllById(reviewIds).stream().map(ReviewInfo::of).toList();
    }
}
