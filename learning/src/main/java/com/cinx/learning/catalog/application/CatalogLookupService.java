package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.AssignmentLessonInfo;
import com.cinx.learning.catalog.application.query.CourseDraftInfo;
import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.catalog.application.query.CourseInstructorMembershipInfo;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.query.VideoLessonInfo;
import com.cinx.learning.catalog.domain.AssignmentLessonRepository;
import com.cinx.learning.catalog.domain.Category;
import com.cinx.learning.catalog.domain.CategoryRepository;
import com.cinx.learning.catalog.domain.CourseDraftRepository;
import com.cinx.learning.catalog.domain.CourseInstructorMembershipRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.CourseReviewNoteRepository;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.QuizQuestionRepository;
import com.cinx.learning.catalog.domain.RejectCourseReasonRepository;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.shared.domain.CourseMembershipStatus;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.shared.domain.ReviewNoteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogLookupService implements CatalogLookup {
    private final CourseRepository courseRepository;
    private final CourseDraftRepository courseDraftRepository;
    private final CourseInstructorMembershipRepository membershipRepository;
    private final CategoryRepository categoryRepository;
    private final LessonRepository lessonRepository;
    private final VideoLessonRepository videoLessonRepository;
    private final AssignmentLessonRepository assignmentLessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final CourseReviewNoteRepository reviewNoteRepository;
    private final RejectCourseReasonRepository rejectCourseReasonRepository;

    @Override
    public Optional<CourseInfo> course(String courseId) {
        return courseRepository.findById(courseId).map(CourseInfo::of);
    }

    @Override
    public List<CourseInfo> courses(Collection<String> courseIds) {
        return courseRepository.findAllById(courseIds).stream().map(CourseInfo::of).toList();
    }

    @Override
    public List<CourseInfo> publishedCourses(List<String> courseIds) {
        return courseRepository.findPublishedByIds(courseIds).stream().map(CourseInfo::of).toList();
    }

    @Override
    public List<String> courseIdsOfInstructor(String instructorId) {
        return courseRepository.findIdsByInstructorId(instructorId);
    }

    @Override
    public long countCoursesByStatus(CourseStatus status) {
        return courseRepository.countByStatus(status);
    }

    @Override
    public long countInstructorsOfPublishedCourses() {
        return courseRepository.countDistinctInstructorsOfPublishedCourses();
    }

    @Override
    public Optional<CourseDraftInfo> draftOfCourse(String courseId) {
        return courseDraftRepository.findByCourse(courseId).map(CourseDraftInfo::of);
    }

    @Override
    public List<CourseInstructorMembershipInfo> activeMembershipsOf(String instructorId) {
        return membershipRepository.findByInstructorIdAndStatus(instructorId, CourseMembershipStatus.ACTIVE).stream()
                .map(CourseInstructorMembershipInfo::of)
                .toList();
    }

    @Override
    public Map<String, String> categoryNames(Collection<String> categoryIds) {
        return categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    @Override
    public List<LessonInfo> readableLessons(String courseId) {
        return lessonRepository.findEnrolledReadableByCourse(courseId).stream().map(LessonInfo::of).toList();
    }

    @Override
    public List<LessonInfo> readableLessonsOfCourses(Collection<String> courseIds) {
        return lessonRepository.findEnrolledReadableByCourseIn(courseIds).stream().map(LessonInfo::of).toList();
    }

    @Override
    public List<LessonInfo> readableLessonsByStableIds(Collection<String> stableIds) {
        return lessonRepository.findReadableByStableIdIn(stableIds).stream().map(LessonInfo::of).toList();
    }

    @Override
    public List<LessonInfo> readableLessonsByStableId(String courseId, String stableId) {
        return lessonRepository.findReadableByCourseAndStableId(courseId, stableId).stream().map(LessonInfo::of).toList();
    }

    @Override
    public List<LessonInfo> publishedLessons(String courseId) {
        return lessonRepository.findPublishedByCourse(courseId).stream().map(LessonInfo::of).toList();
    }

    @Override
    public List<LessonInfo> draftLessons(String draftId) {
        return lessonRepository.findDraftByDraft(draftId).stream().map(LessonInfo::of).toList();
    }

    @Override
    public Optional<VideoLessonInfo> videoLesson(String lessonId) {
        return videoLessonRepository.findByLessonId(lessonId).map(VideoLessonInfo::of);
    }

    @Override
    public List<VideoLessonInfo> videoLessons(Collection<String> lessonIds) {
        return videoLessonRepository.findAllByLessonIdIn(lessonIds).stream().map(VideoLessonInfo::of).toList();
    }

    @Override
    public Optional<AssignmentLessonInfo> assignmentLesson(String lessonId) {
        return assignmentLessonRepository.findById(lessonId).map(AssignmentLessonInfo::of);
    }

    @Override
    public Map<String, Long> quizQuestionCounts(Collection<String> quizLessonIds) {
        return quizQuestionRepository.countPerQuizLesson(quizLessonIds).stream()
                .collect(Collectors.toMap(QuizQuestionRepository.QuizCount::getLessonId, QuizQuestionRepository.QuizCount::getTotal));
    }

    @Override
    public int sentReviewNoteCount(String courseId) {
        return reviewNoteRepository.findAllByCourseIdAndStatusOrderByCreatedAtAsc(courseId, ReviewNoteStatus.SENT).size();
    }

    @Override
    public Optional<LocalDateTime> lastRejectedAt(String courseId) {
        return rejectCourseReasonRepository.findByCourse(courseId)
                .map(reason -> reason.getUpdatedAt() != null ? reason.getUpdatedAt() : reason.getCreatedAt());
    }
}
