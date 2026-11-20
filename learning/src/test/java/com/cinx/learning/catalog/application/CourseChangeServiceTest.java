package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.DraftChanges;
import com.cinx.learning.catalog.application.query.DraftChanges.LessonChangeKind;
import com.cinx.learning.catalog.domain.Category;
import com.cinx.learning.catalog.domain.CategoryRepository;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseDraft;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.Section;
import com.cinx.learning.catalog.domain.SectionRepository;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.shared.domain.LessonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseChangeServiceTest {
    @Mock
    private ICourseAccessService courseAccessService;
    @Mock
    private ICourseDraftService courseDraftService;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CourseChangeService service;

    private final Course course = Course.builder().id("course-1").status(CourseStatus.PUBLISHED).title("Title").categoryId("cat-live").build();
    private final CourseDraft draft = CourseDraft.builder().courseId("course-1").title("Title").categoryId("cat-live").hasCertificate(false).build();

    @BeforeEach
    void setUp() {
        draft.setId("draft-1");
        lenient().when(courseAccessService.ensureManageableCourse("owner-1", "course-1")).thenReturn(course);
        lenient().when(courseDraftService.findDraft(course)).thenReturn(Optional.of(draft));
    }

    @Test
    void aCourseWithoutALiveVersionHasNothingToCompare() {
        Course unpublished = Course.builder().id("course-2").status(CourseStatus.DRAFT).build();
        when(courseAccessService.ensureManageableCourse("owner-1", "course-2")).thenReturn(unpublished);

        DraftChanges changes = service.getDraftChanges("owner-1", "course-2");

        assertThat(changes.hasLiveVersion()).isFalse();
        assertThat(changes.course()).isEmpty();
    }

    @Test
    void aLessonThatSitsInTheSameSectionOfBothVersionsIsNotMoved() {
        sections(List.of(section("live-1", "s1", "Intro")), List.of(section("draft-1", "s1", "Intro")));
        when(lessonRepository.findPublishedByCourse("course-1")).thenReturn(List.of(lesson("l1", "live-1", "A", 0)));
        when(lessonRepository.findDraftByDraft("draft-1")).thenReturn(List.of(lesson("l1", "draft-1", "A", 0)));

        DraftChanges changes = service.getDraftChanges("owner-1", "course-1");

        assertThat(changes.lessons().changed()).isEmpty();
        assertThat(changes.lessons().added()).isEmpty();
        assertThat(changes.lessons().removed()).isEmpty();
    }

    @Test
    void aLessonMovedToAnotherSectionIsReportedWithTheTitleOfItsNewSection() {
        sections(List.of(section("live-1", "s1", "Intro"), section("live-2", "s2", "Advanced")),
                List.of(section("draft-1", "s1", "Intro"), section("draft-2", "s2", "Advanced")));
        when(lessonRepository.findPublishedByCourse("course-1")).thenReturn(List.of(lesson("l1", "live-1", "A", 0)));
        when(lessonRepository.findDraftByDraft("draft-1")).thenReturn(List.of(lesson("l1", "draft-2", "A", 0)));

        DraftChanges changes = service.getDraftChanges("owner-1", "course-1");

        assertThat(changes.lessons().changed()).singleElement().satisfies(change -> {
            assertThat(change.lessonId()).isEqualTo("l1");
            assertThat(change.sectionTitle()).isEqualTo("Advanced");
            assertThat(change.changes()).containsExactly(LessonChangeKind.MOVED);
        });
    }

    @Test
    void addedAndRemovedLessonsCarryTheirSectionTitle() {
        sections(List.of(section("live-1", "s1", "Intro")), List.of(section("draft-1", "s1", "Intro")));
        when(lessonRepository.findPublishedByCourse("course-1")).thenReturn(List.of(lesson("gone", "live-1", "Old", 0)));
        when(lessonRepository.findDraftByDraft("draft-1")).thenReturn(List.of(lesson("fresh", "draft-1", "New", 0)));

        DraftChanges changes = service.getDraftChanges("owner-1", "course-1");

        assertThat(changes.lessons().added()).singleElement().satisfies(ref -> {
            assertThat(ref.lessonId()).isEqualTo("fresh");
            assertThat(ref.sectionTitle()).isEqualTo("Intro");
        });
        assertThat(changes.lessons().removed()).extracting(DraftChanges.LessonRef::lessonId).containsExactly("gone");
    }

    @Test
    void aRetitledLessonIsReportedWithoutBeingMoved() {
        sections(List.of(section("live-1", "s1", "Intro")), List.of(section("draft-1", "s1", "Intro")));
        when(lessonRepository.findPublishedByCourse("course-1")).thenReturn(List.of(lesson("l1", "live-1", "A", 0)));
        when(lessonRepository.findDraftByDraft("draft-1")).thenReturn(List.of(lesson("l1", "draft-1", "A2", 0)));

        DraftChanges changes = service.getDraftChanges("owner-1", "course-1");

        assertThat(changes.lessons().changed()).singleElement()
                .satisfies(change -> assertThat(change.changes()).containsExactly(LessonChangeKind.TITLE));
    }

    @Test
    void aChangedCategoryIsReportedByItsName() {
        draft.setCategoryId("cat-draft");
        sections(List.of(), List.of());
        when(lessonRepository.findPublishedByCourse("course-1")).thenReturn(List.of());
        when(lessonRepository.findDraftByDraft("draft-1")).thenReturn(List.of());
        when(categoryRepository.findById("cat-live")).thenReturn(Optional.of(category("cat-live", "Backend")));
        when(categoryRepository.findById("cat-draft")).thenReturn(Optional.of(category("cat-draft", "Frontend")));

        DraftChanges changes = service.getDraftChanges("owner-1", "course-1");

        assertThat(changes.course()).singleElement().satisfies(change -> {
            assertThat(change.field()).isEqualTo("category");
            assertThat(change.before()).isEqualTo("Backend");
            assertThat(change.after()).isEqualTo("Frontend");
        });
    }

    private void sections(List<Section> live, List<Section> draftSections) {
        when(sectionRepository.findPublishedByCourse("course-1")).thenReturn(live);
        when(sectionRepository.findDraftByDraft("draft-1")).thenReturn(draftSections);
    }

    private static Category category(String id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private static Section section(String id, String stableId, String title) {
        Section section = new Section();
        section.setId(id);
        section.setStableId(stableId);
        section.setTitle(title);
        return section;
    }

    private static Lesson lesson(String stableId, String sectionId, String title, int order) {
        return Lesson.builder().stableId(stableId).sectionId(sectionId).title(title).orderIndex(order).lessonType(LessonType.ARTICLE).build();
    }
}
