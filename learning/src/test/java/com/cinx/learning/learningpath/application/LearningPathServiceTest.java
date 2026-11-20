package com.cinx.learning.learningpath.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.enrollment.application.EnrollmentAccess;
import com.cinx.learning.enrollment.application.query.CheckEnrollmentStatus;
import com.cinx.learning.learningpath.application.command.LearningPathCommand;
import com.cinx.learning.learningpath.application.command.LearningPathItemCommand;
import com.cinx.learning.learningpath.application.mapper.LearningPathMapper;
import com.cinx.learning.learningpath.application.query.LearningPathView;
import com.cinx.learning.learningpath.domain.LearningPathItem;
import com.cinx.learning.learningpath.domain.LearningPathItemRepository;
import com.cinx.learning.learningpath.domain.UserLearningPath;
import com.cinx.learning.learningpath.domain.UserLearningPathRepository;
import com.cinx.learning.progress.application.ProgressLookup;
import com.cinx.learning.progress.application.query.LearningItemProgressInfo;
import com.cinx.learning.shared.domain.LearningPathStatus;
import com.cinx.learning.studyplan.application.IStudyPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningPathServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-30T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UserLearningPathRepository pathRepository;
    @Mock
    private LearningPathItemRepository itemRepository;
    @Mock
    private LearningPathMapper pathMapper;
    @Mock
    private EnrollmentAccess enrollmentAccess;
    @Mock
    private LearningPathCheckouts checkouts;
    @Mock
    private ProgressLookup progressLookup;
    @Mock
    private LearningPathItemEnricher itemEnricher;
    @Mock
    private IStudyPlanService studyPlanService;

    private LearningPathService learningPathService;

    @BeforeEach
    void setUp() {
        learningPathService = new LearningPathService(progressLookup, pathRepository, itemRepository, pathMapper, enrollmentAccess,
                checkouts, itemEnricher, studyPlanService, CLOCK);
    }

    @Test
    void createLearningPathBackfillsCompletedLessons() {
        LearningPathCommand request = LearningPathCommand.builder()
                .title("Backend Path")
                .items(List.of(
                        itemRequest("course-1", "lesson-1", 0),
                        itemRequest("course-1", "lesson-2", 1)))
                .build();
        ArgumentCaptor<UserLearningPath> pathCaptor = ArgumentCaptor.forClass(UserLearningPath.class);
        stubCreate(List.of(completedItem("lesson-1")));
        when(pathRepository.saveAndFlush(pathCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        learningPathService.createLearningPath("user-1", null, request);

        UserLearningPath savedPath = pathCaptor.getValue();
        assertThat(savedPath.getCompletedItems()).isEqualTo(1);
        assertThat(savedPath.getTotalItems()).isEqualTo(2);
        assertThat(savedPath.getCurrentProgress()).isEqualTo(50.0);
        assertThat(savedPath.getStatus()).isEqualTo(LearningPathStatus.ACTIVE);
        assertThat(savedPath.getCreatedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 9, 30, 8, 0));
        assertThat(savedPath.getItems())
                .extracting(LearningPathItem::getLessonId, LearningPathItem::getIsCompleted, LearningPathItem::getOrderIndex)
                .containsExactly(
                        tuple("lesson-1", true, 0),
                        tuple("lesson-2", false, 1));
    }

    @Test
    void createLearningPathMarksCompletedWhenAllLessonsWereAlreadyCompleted() {
        LearningPathCommand request = LearningPathCommand.builder()
                .title("Finished Path")
                .items(List.of(
                        itemRequest("course-1", "lesson-1", 0),
                        itemRequest("course-1", "lesson-2", 1)))
                .build();
        stubCreate(List.of(completedItem("lesson-1"), completedItem("lesson-2")));
        when(pathRepository.saveAndFlush(any(UserLearningPath.class))).thenAnswer(invocation -> invocation.getArgument(0));

        learningPathService.createLearningPath("user-1", null, request);

        ArgumentCaptor<UserLearningPath> pathCaptor = ArgumentCaptor.forClass(UserLearningPath.class);
        verify(pathRepository).saveAndFlush(pathCaptor.capture());
        assertThat(pathCaptor.getValue().getCompletedItems()).isEqualTo(2);
        assertThat(pathCaptor.getValue().getCurrentProgress()).isEqualTo(100.0);
        assertThat(pathCaptor.getValue().getStatus()).isEqualTo(LearningPathStatus.COMPLETED);
        assertThat(pathCaptor.getValue().getCompletedAt()).isNotNull();
    }

    @Test
    void createLearningPathKeepsEachLessonsPhaseTrimmedAndBlankAsNone() {
        LearningPathCommand request = LearningPathCommand.builder()
                .title("Phased Path")
                .items(List.of(
                        phasedItem("course-1", "lesson-1", " Nền tảng "),
                        phasedItem("course-1", "lesson-2", "  ")))
                .build();
        ArgumentCaptor<UserLearningPath> pathCaptor = ArgumentCaptor.forClass(UserLearningPath.class);
        stubCreate(List.of());
        when(pathRepository.saveAndFlush(pathCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        learningPathService.createLearningPath("user-1", null, request);

        assertThat(pathCaptor.getValue().getItems())
                .extracting(LearningPathItem::getLessonId, LearningPathItem::getPhase)
                .containsExactly(tuple("lesson-1", "Nền tảng"), tuple("lesson-2", null));
    }

    @Test
    void replayingAnIdempotencyKeyWithOtherPhasesIsRejected() {
        LearningPathCommand first = LearningPathCommand.builder()
                .title("Phased Path")
                .items(List.of(phasedItem("course-1", "lesson-1", "Nền tảng"), phasedItem("course-1", "lesson-2", "Nền tảng")))
                .build();
        ArgumentCaptor<UserLearningPath> pathCaptor = ArgumentCaptor.forClass(UserLearningPath.class);
        stubCreate(List.of());
        when(pathRepository.findByUserIdAndIdempotencyKey("user-1", "key-1")).thenReturn(Optional.empty());
        when(pathRepository.saveAndFlush(pathCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        learningPathService.createLearningPath("user-1", "key-1", first);

        when(pathRepository.findByUserIdAndIdempotencyKey("user-1", "key-1")).thenReturn(Optional.of(pathCaptor.getValue()));
        LearningPathCommand renamed = LearningPathCommand.builder()
                .title("Phased Path")
                .items(List.of(phasedItem("course-1", "lesson-1", "Nền tảng"), phasedItem("course-1", "lesson-2", "Cơ bản")))
                .build();

        assertThatThrownBy(() -> learningPathService.createLearningPath("user-1", "key-1", renamed))
                .isInstanceOf(BadRequestException.class);
    }

    private LearningPathItemCommand phasedItem(String courseId, String lessonId, String phase) {
        return LearningPathItemCommand.builder().courseId(courseId).lessonId(lessonId).phase(phase).isSuggested(false).build();
    }

    private void stubCreate(List<LearningItemProgressInfo> completed) {
        when(pathRepository.findByUserIdAndStatusIn(
                "user-1", List.of(LearningPathStatus.ACTIVE, LearningPathStatus.PENDING_PAYMENT)))
                .thenReturn(Optional.empty());
        when(enrollmentAccess.entitledCourseIds("user-1", List.of("course-1")))
                .thenReturn(new HashSet<>());
        when(enrollmentAccess.checkEnrollmentStatus("user-1", List.of("course-1")))
                .thenReturn(new ApiResponse<>(true, "ok", List.of(new CheckEnrollmentStatus("course-1", true))));
        when(progressLookup.itemProgressOfUser("user-1", List.of("lesson-1", "lesson-2")))
                .thenReturn(completed);
        when(pathMapper.toDto(any(UserLearningPath.class))).thenReturn(new LearningPathView());
        when(itemEnricher.enrich(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    private LearningPathItemCommand itemRequest(String courseId, String lessonId, int orderIndex) {
        return LearningPathItemCommand.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .orderIndex(orderIndex)
                .isSuggested(false)
                .build();
    }

    private LearningItemProgressInfo completedItem(String itemId) {
        return LearningItemProgressInfo.builder()
                .itemId(itemId)
                .isCompleted(true)
                .build();
    }
}
