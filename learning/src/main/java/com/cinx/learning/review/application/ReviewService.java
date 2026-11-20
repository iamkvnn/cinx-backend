package com.cinx.learning.review.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.assessment.application.QuizPractice;
import com.cinx.learning.assessment.application.query.PracticeCheckView;
import com.cinx.learning.assessment.application.query.PracticeQuestionView;
import com.cinx.learning.catalog.application.CatalogLookup;
import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.engagement.application.ILearningActivityService;
import com.cinx.learning.engagement.application.IStreakService;
import com.cinx.learning.review.application.command.QuizQuestionResult;
import com.cinx.learning.review.application.query.ReviewAnswerView;
import com.cinx.learning.review.application.query.ReviewTodayView;
import com.cinx.learning.review.domain.ReviewAttempt;
import com.cinx.learning.review.domain.ReviewAttemptRepository;
import com.cinx.learning.review.domain.ReviewItem;
import com.cinx.learning.review.domain.ReviewItemRepository;
import com.cinx.learning.review.domain.ReviewItemStatus;
import com.cinx.learning.studyplan.application.StudyPlanScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {
    // due items read per page: enough to fill the cap past questions skipped for access
    private static final int SCAN = DAILY_CAP * 3;

    private final ReviewItemRepository items;
    private final ReviewAttemptRepository attempts;
    private final QuizPractice practice;
    private final CatalogLookup catalog;
    private final ILearningActivityService activity;
    private final IStreakService streaks;
    private final Clock clock;

    @Override
    @Transactional
    public void recordQuizResults(String userId, String courseId, String lessonId, List<QuizQuestionResult> results) {
        Map<String, QuizQuestionResult> missed = results.stream()
                .filter(result -> result.score() < 1.0)
                .collect(Collectors.toMap(QuizQuestionResult::questionId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        if (missed.isEmpty()) {
            return;
        }
        LocalDate today = today();
        LocalDateTime now = now();
        Map<String, ReviewItem> known = items.findByUserIdAndQuestionIdIn(userId, missed.keySet()).stream()
                .collect(Collectors.toMap(ReviewItem::getQuestionId, Function.identity()));
        List<ReviewItem> changed = new ArrayList<>();
        for (String questionId : missed.keySet()) {
            ReviewItem existing = known.get(questionId);
            changed.add(existing == null
                    ? ReviewItem.missedInQuiz(UUID.randomUUID().toString(), userId, courseId, lessonId, questionId, today, now)
                    : existing.missedAgain(today, now));
        }
        items.saveAll(changed);
    }

    @Override
    @Transactional
    public ReviewTodayView today(String userId) {
        LocalDate today = today();
        LocalDateTime now = now();
        Map<String, Optional<Map<String, PracticeQuestionView>>> lessons = new HashMap<>();
        Map<String, String> courseTitles = new HashMap<>();
        Map<String, String> lessonTitles = new HashMap<>();
        List<ReviewItem> retired = new ArrayList<>();
        List<ReviewTodayView.Item> shown = new ArrayList<>();

        for (ReviewItem item : items.findDue(userId, today, SCAN)) {
            if (shown.size() >= DAILY_CAP) {
                break;
            }
            Optional<Map<String, PracticeQuestionView>> questions = lessons.computeIfAbsent(
                    item.getCourseId() + "/" + item.getLessonId(), key -> practice.questions(item.getCourseId(), item.getLessonId()));
            if (questions.isEmpty()) {
                // no access right now (an ended subscription): the question waits rather than leaving the queue
                continue;
            }
            PracticeQuestionView question = questions.get().get(item.getQuestionId());
            if (question == null) {
                retired.add(item.retired(now));
                continue;
            }
            shown.add(new ReviewTodayView.Item(item.getId(), item.getCourseId(),
                    courseTitles.computeIfAbsent(item.getCourseId(), this::courseTitle), item.getLessonId(),
                    lessonTitles.computeIfAbsent(item.getCourseId() + "/" + item.getLessonId(),
                            key -> lessonTitle(item.getCourseId(), item.getLessonId())),
                    item.getBox(), question.questionId(), question.questionText(), question.questionType(),
                    question.scoringMethod(), question.options()));
        }
        if (!retired.isEmpty()) {
            items.saveAll(retired);
        }
        return new ReviewTodayView(items.countDue(userId, today), DAILY_CAP, shown);
    }

    @Override
    @Transactional
    public ReviewAnswerView answer(String userId, String reviewItemId, String userAnswer, Integer elapsedSeconds,
                                  String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 255) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "A valid Idempotency-Key header is required");
        }
        String fingerprint = fingerprint(reviewItemId, userAnswer);
        Optional<ReviewAttempt> earlier = attempts.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (earlier.isPresent()) {
            if (!earlier.get().getRequestFingerprint().equals(fingerprint)) {
                throw new ConflictException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "This key was used for another review answer");
            }
            return replay(userId, earlier.get());
        }

        ReviewItem item = items.findByIdAndUserId(reviewItemId, userId)
                .orElseThrow(() -> new NotFoundException("Review item not found"));
        LocalDate today = today();
        if (!item.dueBy(today)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "This question is not due for review");
        }
        PracticeCheckView checked = practice.check(item.getCourseId(), item.getLessonId(), item.getQuestionId(), userAnswer)
                .orElseThrow(() -> new NotFoundException("This question is no longer available for review"));
        LocalDateTime now = now();
        ReviewItem next = items.save(item.answered(checked.correct(), today, now));
        boolean graduated = next.getStatus() == ReviewItemStatus.GRADUATED;
        attempts.insert(ReviewAttempt.builder()
                .id(UUID.randomUUID().toString())
                .reviewItemId(item.getId())
                .userId(userId)
                .questionId(item.getQuestionId())
                .userAnswer(userAnswer)
                .score(BigDecimal.valueOf(checked.score()).setScale(4, RoundingMode.HALF_UP))
                .correct(checked.correct())
                .boxBefore(item.getBox())
                .boxAfter(next.getBox())
                .dueOnAfter(graduated ? null : next.getDueOn())
                .answeredAt(now)
                .idempotencyKey(idempotencyKey)
                .requestFingerprint(fingerprint)
                .build());

        // reviewing is studying: it keeps the streak and the day's activity, never an official score
        int seconds = Math.max(0, Math.min(MAX_SECONDS_PER_ANSWER, elapsedSeconds == null ? 0 : elapsedSeconds));
        if (seconds > 0) {
            activity.recordActivity(userId, item.getCourseId(), seconds);
        }
        streaks.updateStreakOnActivity(userId);

        return new ReviewAnswerView(checked.correct(), checked.score(), checked.correctAnswer(), checked.explanation(),
                next.getBox(), graduated ? null : next.getDueOn(), graduated);
    }

    @Override
    @Transactional(readOnly = true)
    public long dueCount(String userId, LocalDate day) {
        return items.countDue(userId, day);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> userIdsWithDue(LocalDate day) {
        return items.findUserIdsWithDue(day);
    }

    /** The stored outcome of a repeated request; the key and explanation are read again, the schedule is not touched. */
    private ReviewAnswerView replay(String userId, ReviewAttempt attempt) {
        Optional<ReviewItem> item = items.findByIdAndUserId(attempt.getReviewItemId(), userId);
        Optional<PracticeCheckView> shown = item.flatMap(i ->
                practice.check(i.getCourseId(), i.getLessonId(), i.getQuestionId(), attempt.getUserAnswer()));
        return new ReviewAnswerView(attempt.isCorrect(), attempt.getScore().doubleValue(),
                shown.map(PracticeCheckView::correctAnswer).orElse(null), shown.map(PracticeCheckView::explanation).orElse(null),
                attempt.getBoxAfter(), attempt.getDueOnAfter(), attempt.getDueOnAfter() == null);
    }

    private String courseTitle(String courseId) {
        return catalog.course(courseId).map(CourseInfo::getTitle).orElse(null);
    }

    private String lessonTitle(String courseId, String lessonId) {
        return catalog.readableLessonsByStableId(courseId, lessonId).stream().findFirst().map(LessonInfo::getTitle).orElse(null);
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(StudyPlanScheduler.ZONE));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String fingerprint(String reviewItemId, String userAnswer) {
        String payload = reviewItemId + "\u001f" + (userAnswer == null ? "" : userAnswer);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
