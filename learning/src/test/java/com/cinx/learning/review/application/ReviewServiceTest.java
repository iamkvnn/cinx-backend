package com.cinx.learning.review.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.assessment.application.QuizPractice;
import com.cinx.learning.assessment.application.query.PracticeCheckView;
import com.cinx.learning.assessment.application.query.PracticeQuestionView;
import com.cinx.learning.assessment.application.query.QuizSessionOptionView;
import com.cinx.learning.catalog.application.CatalogLookup;
import com.cinx.learning.catalog.application.query.CourseInfo;
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
import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {
    // 03:00 UTC is 10:00 on 13 October in Asia/Ho_Chi_Minh
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    @Mock private ReviewItemRepository items;
    @Mock private ReviewAttemptRepository attempts;
    @Mock private QuizPractice practice;
    @Mock private CatalogLookup catalog;
    @Mock private ILearningActivityService activity;
    @Mock private IStreakService streaks;

    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(items, attempts, practice, catalog, activity, streaks, CLOCK);
        doAnswer(invocation -> invocation.getArgument(0)).when(items).save(any());
        doAnswer(invocation -> invocation.getArgument(0)).when(items).saveAll(any());
        doAnswer(invocation -> invocation.getArgument(0)).when(attempts).insert(any());
        doReturn(Optional.empty()).when(attempts).findByUserIdAndIdempotencyKey(anyString(), anyString());
        doReturn(Optional.of(CourseInfo.builder().id("c1").title("Python").build())).when(catalog).course("c1");
        doReturn(List.of()).when(catalog).readableLessonsByStableId(anyString(), anyString());
    }

    @Test
    void onlyMissedQuestionsEnterAndAKnownOneStartsOver() {
        ReviewItem known = ReviewItem.missedInQuiz("r-old", "u1", "c1", "l1", "q2", TODAY.minusDays(20), LocalDateTime.now(CLOCK))
                .toBuilder().box(4).status(ReviewItemStatus.GRADUATED).build();
        doReturn(List.of(known)).when(items).findByUserIdAndQuestionIdIn(any(), any());

        service.recordQuizResults("u1", "c1", "l1", List.of(
                new QuizQuestionResult("q1", 0.0), new QuizQuestionResult("q2", 0.5), new QuizQuestionResult("q3", 1.0)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReviewItem>> saved = ArgumentCaptor.forClass(List.class);
        verify(items).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ReviewItem::getQuestionId).containsExactly("q1", "q2");
        assertThat(saved.getValue()).allSatisfy(item -> {
            assertThat(item.getBox()).isEqualTo(1);
            assertThat(item.getStatus()).isEqualTo(ReviewItemStatus.ACTIVE);
            assertThat(item.getDueOn()).isEqualTo(TODAY.plusDays(1));
        });
        assertThat(saved.getValue().get(1).getId()).isEqualTo("r-old");
    }

    @Test
    void aFullyRightQuizTouchesNothing() {
        service.recordQuizResults("u1", "c1", "l1", List.of(new QuizQuestionResult("q1", 1.0)));
        verifyNoInteractions(items);
    }

    @Test
    void todayCapsTheQueueAndShowsNoKey() {
        List<ReviewItem> due = IntStream.range(0, 25).mapToObj(i -> dueItem("r" + i, "q" + i)).toList();
        doReturn(due).when(items).findDue("u1", TODAY, 60);
        doReturn(25L).when(items).countDue("u1", TODAY);
        Map<String, PracticeQuestionView> questions = new java.util.HashMap<>();
        due.forEach(item -> questions.put(item.getQuestionId(), question(item.getQuestionId())));
        doReturn(Optional.of(questions)).when(practice).questions("c1", "l1");

        ReviewTodayView today = service.today("u1");

        assertThat(today.items()).hasSize(IReviewService.DAILY_CAP);
        assertThat(today.dueTotal()).isEqualTo(25);
        assertThat(today.items().get(0).courseTitle()).isEqualTo("Python");
        assertThat(today.items().get(0).options()).extracting(QuizSessionOptionView::optionText).containsExactly("A", "B");
        verify(practice, org.mockito.Mockito.times(1)).questions("c1", "l1");
    }

    @Test
    void aGoneQuestionIsRetiredAndACourseWithoutAccessWaits() {
        ReviewItem gone = dueItem("r1", "q-gone");
        ReviewItem locked = dueItem("r2", "q2").toBuilder().courseId("c-locked").build();
        doReturn(List.of(gone, locked)).when(items).findDue("u1", TODAY, 60);
        doReturn(Optional.of(Map.of())).when(practice).questions("c1", "l1");
        doReturn(Optional.empty()).when(practice).questions("c-locked", "l1");

        ReviewTodayView today = service.today("u1");

        assertThat(today.items()).isEmpty();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReviewItem>> retired = ArgumentCaptor.forClass(List.class);
        verify(items).saveAll(retired.capture());
        assertThat(retired.getValue()).extracting(ReviewItem::getId).containsExactly("r1");
        assertThat(retired.getValue().get(0).getStatus()).isEqualTo(ReviewItemStatus.RETIRED);
    }

    @Test
    void aRightAnswerMovesUpAndCountsAsStudyButNeverAsAScore() {
        doReturn(Optional.of(dueItem("r1", "q1"))).when(items).findByIdAndUserId("r1", "u1");
        doReturn(Optional.of(new PracticeCheckView(1.0, "[\"a\"]", "Vì A đúng"))).when(practice).check("c1", "l1", "q1", "[\"a\"]");

        ReviewAnswerView answer = service.answer("u1", "r1", "[\"a\"]", 900, "key-1");

        assertThat(answer.correct()).isTrue();
        assertThat(answer.box()).isEqualTo(2);
        assertThat(answer.nextDueOn()).isEqualTo(TODAY.plusDays(3));
        assertThat(answer.explanation()).isEqualTo("Vì A đúng");
        ArgumentCaptor<ReviewAttempt> attempt = ArgumentCaptor.forClass(ReviewAttempt.class);
        verify(attempts).insert(attempt.capture());
        assertThat(attempt.getValue().getScore()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat((attempt.getValue().getBoxBefore())).isEqualTo(1);
        // a question left open for 15 minutes counts as 5
        verify(activity).recordActivity("u1", "c1", IReviewService.MAX_SECONDS_PER_ANSWER);
        verify(streaks).updateStreakOnActivity("u1");
    }

    @Test
    void aWrongAnswerStartsOverTomorrow() {
        doReturn(Optional.of(dueItem("r1", "q1").toBuilder().box(3).build())).when(items).findByIdAndUserId("r1", "u1");
        doReturn(Optional.of(new PracticeCheckView(0.5, "[\"a\",\"b\"]", null))).when(practice).check(any(), any(), any(), any());

        ReviewAnswerView answer = service.answer("u1", "r1", "[\"a\"]", 20, "key-1");

        assertThat(answer.correct()).isFalse();
        assertThat(answer.box()).isEqualTo(1);
        assertThat(answer.nextDueOn()).isEqualTo(TODAY.plusDays(1));
        verify(activity).recordActivity("u1", "c1", 20);
    }

    @Test
    void anotherLearnersItemIsNotFound() {
        doReturn(Optional.empty()).when(items).findByIdAndUserId("r1", "u2");

        assertThatThrownBy(() -> service.answer("u2", "r1", "[]", 5, "key-1")).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(practice, activity, streaks);
    }

    @Test
    void aQuestionNotYetDueIsRefused() {
        doReturn(Optional.of(dueItem("r1", "q1").toBuilder().dueOn(TODAY.plusDays(2)).build())).when(items).findByIdAndUserId("r1", "u1");

        assertThatThrownBy(() -> service.answer("u1", "r1", "[]", 5, "key-1")).isInstanceOf(BadRequestException.class);
        verify(items, never()).save(any());
    }

    @Test
    void theSameKeyAndBodyReplayTheStoredResultAndAnotherBodyConflicts() {
        ReviewAttempt stored = ReviewAttempt.builder().reviewItemId("r1").userId("u1").questionId("q1").userAnswer("[\"a\"]")
                .score(BigDecimal.ONE).correct(true).boxBefore(1).boxAfter(2).dueOnAfter(TODAY.plusDays(3))
                .answeredAt(LocalDateTime.now(CLOCK)).idempotencyKey("key-1")
                .requestFingerprint(fingerprintOf("r1", "[\"a\"]")).build();
        doReturn(Optional.of(stored)).when(attempts).findByUserIdAndIdempotencyKey("u1", "key-1");
        doReturn(Optional.of(dueItem("r1", "q1"))).when(items).findByIdAndUserId("r1", "u1");
        doReturn(Optional.of(new PracticeCheckView(1.0, "[\"a\"]", "Vì A đúng"))).when(practice).check(any(), any(), any(), any());

        ReviewAnswerView replay = service.answer("u1", "r1", "[\"a\"]", 10, "key-1");

        assertThat((replay.box())).isEqualTo(2);
        assertThat(replay.correctAnswer()).isEqualTo("[\"a\"]");
        verify(items, never()).save(any());
        verifyNoInteractions(activity, streaks);
        assertThatThrownBy(() -> service.answer("u1", "r1", "[\"b\"]", 10, "key-1")).isInstanceOf(ConflictException.class);
    }

    @Test
    void anAnswerWithoutAKeyIsRefused() {
        assertThatThrownBy(() -> service.answer("u1", "r1", "[]", 5, " ")).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(items);
    }

    private static ReviewItem dueItem(String id, String questionId) {
        return ReviewItem.missedInQuiz(id, "u1", "c1", "l1", questionId, TODAY.minusDays(1), LocalDateTime.now(CLOCK));
    }

    private static PracticeQuestionView question(String id) {
        return new PracticeQuestionView(id, "Câu " + id, QuizQuestionType.SINGLE_CHOICE, ScoringMethod.ALL_OR_NOTHING,
                List.of(new QuizSessionOptionView("o1", "A", null), new QuizSessionOptionView("o2", "B", null)));
    }

    private static String fingerprintOf(String itemId, String answer) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest((itemId + "\u001f" + answer).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
