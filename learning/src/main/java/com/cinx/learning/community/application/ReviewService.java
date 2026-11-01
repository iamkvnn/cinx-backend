package com.cinx.learning.community.application;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import com.cinx.common.exception.NotFoundException;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.enrollment.application.EnrollmentAccess;
import com.cinx.learning.community.application.UserSummaryDirectory;
import com.cinx.learning.community.application.command.CreateReportReviewCommand;
import com.cinx.learning.community.application.command.CreateReviewReactionCommand;
import com.cinx.learning.community.application.command.CreateReviewCommand;
import com.cinx.learning.community.application.command.UpdateReviewCommand;
import com.cinx.learning.community.application.query.ReviewReactionView;
import com.cinx.learning.community.application.query.ReviewView;
import com.cinx.learning.enrollment.application.query.CheckEnrollmentStatus;
import com.cinx.learning.community.application.query.UserSummaryView;
import com.cinx.learning.community.application.event.CourseReviewCreatedEvent;
import com.cinx.learning.community.application.CourseReviewEvents;
import com.cinx.learning.community.domain.Report;
import com.cinx.learning.community.domain.ReportType;
import com.cinx.learning.community.domain.Review;
import com.cinx.learning.community.domain.ReviewReaction;
import com.cinx.learning.community.domain.ReportRepository;
import com.cinx.learning.community.domain.ReviewReactionRepository;
import com.cinx.learning.community.domain.ReviewRepository;
import com.cinx.learning.shared.domain.ActivityAction;
import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.application.IInstructorActivityService;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cinx.learning.community.application.command.CreateReviewReplyCommand;
import com.cinx.learning.community.application.command.UpdateReviewReplyCommand;
import com.cinx.learning.community.application.query.ReviewReplyView;
import com.cinx.learning.community.domain.ReviewReply;
import com.cinx.learning.community.domain.ReviewReplyRepository;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.dto.ApiResponse;

@RequiredArgsConstructor
@Service
@Slf4j
public class ReviewService implements IReviewService {
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final CourseService courseService;
    private final EnrollmentAccess enrollmentAccess;
    private final UserSummaryDirectory userClient;
    private final CourseReviewEvents reviewEventPublisher;
    private final LearningAuthorizationService authorizationService;
    private final IInstructorActivityService activityService;

    @Override
    public PageResult<ReviewView> getReviewsByCourseId(String courseId, int page, int size, String sort, String currentUserId, boolean unreplied) {
        PageQuery pageable = new PageQuery(page, size, sort);
        // "unreplied" is an instructor's work filter; for anyone else the public list is returned unchanged.
        boolean onlyUnreplied = unreplied && currentUserId != null && authorizationService.isCourseInstructor(courseId, currentUserId);
        PageResult<Review> reviews = onlyUnreplied
                ? reviewRepository.findUnrepliedByCourseId(courseId, pageable)
                : reviewRepository.findByCourseId(courseId, pageable);
        List<String> reviewIds = reviews.content().stream()
                .map(Review::getId)
                .toList();
        if (reviewIds.isEmpty()) {
            return reviews.map(review -> toReviewResponse(review, null, List.of(), Collections.emptyMap()));
        }
        Map<String, ReviewReply> repliesByReviewId = reviewReplyRepository.findByReviewIdIn(reviewIds).stream()
                .collect(Collectors.toMap(ReviewReply::getReviewId, Function.identity(), (first, second) -> first));
        Map<String, List<ReviewReaction>> reactionsByReviewId = reviewReactionRepository.findByReviewIdIn(reviewIds).stream()
                .collect(Collectors.groupingBy(ReviewReaction::getReviewId));
        // the list is public (guests included), so reviewers, repliers and reactors lose their email
        Map<String, UserSummaryView> usersById = fetchUsersById(reviews.content(), repliesByReviewId.values(), reactionsByReviewId.values().stream()
                .flatMap(Collection::stream)
                .toList()).values().stream()
                .collect(Collectors.toMap(UserSummaryView::userId, UserSummaryView::withoutEmail, (first, second) -> first));

        return reviews.map(review -> toReviewResponse(
                review,
                repliesByReviewId.get(review.getId()),
                reactionsByReviewId.getOrDefault(review.getId(), List.of()),
                usersById));
    }

    @Override
    public ReviewView getReviewById(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        ReviewReply reply = reviewReplyRepository.findByReviewId(review.getId()).orElse(null);
        List<ReviewReaction> reactions = reviewReactionRepository.findByReviewId(review.getId());
        Map<String, UserSummaryView> usersById = fetchUsersById(List.of(review), Optional.ofNullable(reply).stream().toList(), reactions);
        return toReviewResponse(review, reply, reactions, usersById);
    }

    @Override
    @Transactional
    public void createReview(String userId, CreateReviewCommand request) {
        validateRating(request.rating());
        requireEnrolled(userId, request.courseId());
        if (reviewRepository.existsByUserIdAndCourseId(userId, request.courseId())) {
            throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "You have already reviewed this course");
        }
        Review review = reviewRepository.save(Review.builder()
                .courseId(request.courseId())
                .userId(userId)
                .content(request.content())
                .rating(request.rating())
                .build());

        updateCourseRatingInCourseService(request.courseId());
        reviewEventPublisher.publishReviewCreatedEvent(CourseReviewCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .reviewId(review.getId())
                .courseId(review.getCourseId())
                .reviewerUserId(userId)
                .rating(review.getRating())
                .content(review.getContent())
                .occurredAt(Instant.now())
                .build());
    }

    private void updateCourseRatingInCourseService(String courseId) {
        Double averageRating = reviewRepository.getAverageRatingByCourseId(courseId);
        Long ratingCount = reviewRepository.countByCourseId(courseId);
        try {
            courseService.updateCourseRatingStats(courseId, averageRating, ratingCount == null ? 0L : ratingCount);
        } catch (Exception e) {
            // Log issue
        }
    }

    @Override
    @Transactional
    public void updateReview(String userId, String reviewId, UpdateReviewCommand request) {
        validateRating(request.rating());
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (!review.getUserId().equals(userId)) {
            throw new NotFoundException("Review not found");
        }
        review.setContent(request.content());
        review.setRating(request.rating());
        reviewRepository.save(review);
        updateCourseRatingInCourseService(review.getCourseId());
    }

    @Override
    @Transactional
    public void deleteReview(String userId, String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (!review.getUserId().equals(userId)) {
            throw new NotFoundException("Review not found");
        }
        deleteReviewContent(review);
    }

    @Override
    @Transactional
    public void deleteReviewByAdmin(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        deleteReviewContent(review);
    }

    private void deleteReviewContent(Review review) {
        String reviewId = review.getId();
        reviewReactionRepository.deleteByReviewId(reviewId);
        reviewReplyRepository.deleteByReviewId(reviewId);
        reportRepository.deleteByRefIdAndType(reviewId, ReportType.REVIEW);
        reviewRepository.delete(review);
        updateCourseRatingInCourseService(review.getCourseId());
    }

    @Override
    @Transactional
    public void reportReview(String userId, String reviewId, CreateReportReviewCommand request) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new NotFoundException("Review not found");
        }
        reportRepository.save(Report.builder()
                .refId(reviewId)
                .type(ReportType.REVIEW)
                .reporterId(userId)
                .reason(request.reason())
                .build());
    }

    @Override
    @Transactional
    public void reactReview(String userId, String reviewId, CreateReviewReactionCommand request) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new NotFoundException("Review not found");
        }
        ReviewReaction existingReaction = reviewReactionRepository.findByUserIdAndReviewId(userId, reviewId)
                .orElse(null);
        if (existingReaction != null) {
            existingReaction.setLiked(request.liked());
            reviewReactionRepository.save(existingReaction);
            return;
        }
        reviewReactionRepository.save(ReviewReaction.builder()
                .reviewId(reviewId)
                .userId(userId)
                .liked(request.liked())
                .build());
    }

    @Override
    @Transactional
    public void createReviewReply(String userId, String reviewId, CreateReviewReplyCommand request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        
        if (!authorizationService.isCourseInstructor(review.getCourseId(), userId)) {
            throw new ForbiddenException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED, "Only the instructor of the course can reply to this review");
        }

        if (reviewReplyRepository.findByReviewId(reviewId).isPresent()) {
            throw new com.cinx.common.exception.AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Reply already exists for this review");
        }

        ReviewReply reply = new ReviewReply();
        reply.setReviewId(reviewId);
        reply.setInstructorId(userId);
        reply.setContent(request.getContent());
        reviewReplyRepository.save(reply);
        activityService.record(userId, review.getCourseId(), InboxKind.REVIEWS, ActivityAction.REVIEW_REPLIED,
                review.getCourseId(), 1, "Review " + review.getRating());
    }

    @Override
    @Transactional
    public void updateReviewReply(String userId, String replyId, UpdateReviewReplyCommand request) {
        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new NotFoundException("Reply not found"));
        
        if (!reply.getInstructorId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "Not the owner of this reply");
        }

        reply.setContent(request.getContent());
        reviewReplyRepository.save(reply);
    }

    @Override
    @Transactional
    public void deleteReviewReply(String userId, String replyId) {
        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new NotFoundException("Reply not found"));
        
        if (!reply.getInstructorId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "Not the owner of this reply");
        }

        reviewReplyRepository.delete(reply);
    }

    private ReviewView toReviewResponse(
            Review review,
            ReviewReply reply,
            List<ReviewReaction> reactions,
            Map<String, UserSummaryView> usersById) {
        ReviewReplyView replyDto = reply != null ? toReviewReplyDto(reply, usersById) : null;
        List<ReviewReactionView> reactionResponses = reactions.stream()
                .map(reaction -> new ReviewReactionView(
                        reaction.getId(),
                        reaction.getUserId(),
                        usersById.get(reaction.getUserId()),
                        reaction.getReviewId(),
                        reaction.isLiked()))
                .toList();

        return new ReviewView(
                review.getId(),
                review.getUserId(),
                usersById.get(review.getUserId()),
                review.getCourseId(),
                review.getContent(),
                review.getRating(),
                replyDto,
                reactionResponses,
                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    private ReviewReplyView toReviewReplyDto(ReviewReply reply, Map<String, UserSummaryView> usersById) {
        ReviewReplyView replyDto = new ReviewReplyView();
        replyDto.setId(reply.getId());
        replyDto.setReviewId(reply.getReviewId());
        replyDto.setInstructorId(reply.getInstructorId());
        replyDto.setInstructor(usersById.get(reply.getInstructorId()));
        replyDto.setContent(reply.getContent());
        replyDto.setCreatedAt(reply.getCreatedAt());
        replyDto.setUpdatedAt(reply.getUpdatedAt());
        return replyDto;
    }

    private Map<String, UserSummaryView> fetchUsersById(
            List<Review> reviews,
            Collection<ReviewReply> replies,
            List<ReviewReaction> reactions) {
        LinkedHashSet<String> userIds = new LinkedHashSet<>();
        reviews.stream()
                .map(Review::getUserId)
                .forEach(userIds::add);
        replies.stream()
                .map(ReviewReply::getInstructorId)
                .forEach(userIds::add);
        reactions.stream()
                .map(ReviewReaction::getUserId)
                .forEach(userIds::add);

        userIds.remove(null);
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        ApiResponse<List<UserSummaryView>> response;
        try {
            response = userClient.getUsersByIds(List.copyOf(userIds));
        } catch (Exception ex) {
            log.warn("Failed to fetch review user summaries", ex);
            return Collections.emptyMap();
        }
        if (response == null || !response.success() || response.data() == null) {
            return Collections.emptyMap();
        }

        return response.data().stream()
                .filter(user -> user.userId() != null)
                .collect(Collectors.toMap(UserSummaryView::userId, Function.identity(), (first, second) -> first));
    }

    private void validateRating(Double rating) {
        if (rating == null || rating < 1.0 || rating > 5.0) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Rating must be between 1 and 5");
        }
    }

    private void requireEnrolled(String userId, String courseId) {
        ApiResponse<List<CheckEnrollmentStatus>> response = enrollmentAccess.checkEnrollmentStatus(userId, List.of(courseId));
        boolean enrolled = response != null && response.success() && response.data() != null
                && response.data().stream()
                .anyMatch(status -> courseId.equals(status.courseId()) && status.isEnrolled());
        if (!enrolled) {
            throw new BadRequestException(ErrorCode.NOT_ENROLLED_IN_COURSE, "You must enroll in this course before reviewing it");
        }
    }
}
