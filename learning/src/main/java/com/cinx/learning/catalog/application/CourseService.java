package com.cinx.learning.catalog.application;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import com.cinx.learning.catalog.application.query.CourseReadiness;
import com.cinx.learning.catalog.application.query.CourseView;
import com.cinx.learning.catalog.application.query.InstructorCourseSummaryView;
import com.cinx.learning.catalog.application.query.RejectCourseView;
import com.cinx.learning.catalog.application.query.ReviewNoteView;
import com.cinx.learning.catalog.application.query.UserView;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.mapper.SortConverter;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.application.command.CreateCourseCommand;
import com.cinx.learning.catalog.application.command.RejectCourseCommand;
import com.cinx.learning.catalog.application.command.UpdateCourseCommand;
import com.cinx.learning.catalog.application.mapper.CourseMapper;
import com.cinx.learning.catalog.application.CourseEvents;
import com.cinx.learning.catalog.application.event.CourseApprovalRequestedEvent;
import com.cinx.learning.catalog.application.event.CourseContentPublishedEvent;
import com.cinx.learning.catalog.application.event.CourseRecommendationEvent;
import com.cinx.learning.catalog.application.event.CourseRecommendationPayload;
import com.cinx.learning.catalog.application.event.CourseSellabilityChanged;
import com.cinx.learning.catalog.application.event.CourseTranscriptsChanged;
import com.cinx.learning.catalog.domain.Category;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseDraft;
import com.cinx.learning.catalog.domain.RejectCourseReason;
import com.cinx.learning.catalog.domain.CategoryRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.RejectCourseReasonRepository;
import com.cinx.learning.catalog.application.InstructorDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService implements ICourseService {
    private final CourseRepository courseRepository;
    private final RejectCourseReasonRepository rejectCourseReasonRepository;
    private final ICourseReviewNoteService reviewNoteService;
    private final CategoryRepository categoryRepository;
    private final ICourseDraftService courseDraftService;
    private final ICourseReadinessService courseReadinessService;
    private final InstructorDirectory userService;
    private final ICourseAccessService courseAccessService;
    private final ICurriculumService curriculumService;
    private final CourseMapper courseMapper;
    private final CourseEvents courseEventProducer;
    private final ICourseCollaborationService courseCollaborationService;
    private final Clock clock;

    private void publishSellability(Course course) {
        BigDecimal effectivePrice = course.getDiscountedPrice() == null ? course.getPrice() : course.getDiscountedPrice();
        courseEventProducer.publishSellabilityChanged(new CourseSellabilityChanged(
                UUID.randomUUID().toString(), course.getId(), course.getTitle(), effectivePrice,
                course.getCategoryId(), course.getInstructorId(),
                course.getStatus() == CourseStatus.PUBLISHED, LocalDateTime.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView getReadableCourseById(String currentUserId, String courseId) {
        return toResponse(courseAccessService.ensureReadableCourse(currentUserId, courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView getPublicCourseById(String currentUserId, String courseId) {
        return publicView(toResponse(courseAccessService.ensureViewableCourse(currentUserId, courseId)));
    }

    /** Public catalog reads hide the instructor's email from everyone but admins (the admin app reads these endpoints too). */
    private CourseView publicView(CourseView response) {
        return courseAccessService.isAdmin() ? response : response.withoutInstructorEmail();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView getEditableDraftCourseById(String currentUserId, String courseId) {
        Course course = courseAccessService.ensureManageableCourse(currentUserId, courseId);
        return draftOnlyResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseReadiness getReadiness(String currentUserId, String courseId) {
        return courseReadinessService.check(courseAccessService.ensureManageableCourse(currentUserId, courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView getDraftForReview(String courseId) {
        return draftOnlyResponse(courseAccessService.reviewableCourse(courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseReadiness getReadinessForReview(String courseId) {
        return courseReadinessService.check(courseAccessService.reviewableCourse(courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView getCourseForService(String courseId) {
        return toResponse(courseAccessService.ensureViewableCourse(null, courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseView> getCoursesForService(List<String> courseIds) {
        List<String> distinctCourseIds = courseIds.stream().distinct().toList();
        Map<String, Course> courseMap = courseRepository.findEnrolledReadableByIds(distinctCourseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        return toResponse(distinctCourseIds.stream().map(courseMap::get).filter(Objects::nonNull).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseView> getReadableCourseByIds(String currentUserId, List<String> courseIds) {
        // catalog cards for cart, wishlist and paths list unpurchased courses, so content access must not filter them
        return getCoursesForService(courseIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> searchReadableCourseIds(List<String> courseIds, String query) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return courseRepository.searchEnrolledReadableIds(courseIds.stream().distinct().toList(), normalizedQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCourseIdsByInstructor(String instructorId) {
        if (instructorId == null || instructorId.isBlank()) {
            return List.of();
        }
        return courseRepository.findIdsByInstructorId(instructorId);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorCourseSummaryView getInstructorCourseSummary(String instructorId) {
        long courseCount = courseRepository.countByInstructorId(instructorId);
        long publishedCourseCount = courseRepository.countByInstructorIdAndStatus(instructorId, CourseStatus.PUBLISHED);
        Double averageRating = courseRepository.averageRatingByInstructorId(instructorId);
        return new InstructorCourseSummaryView(
                courseCount,
                publishedCourseCount,
                averageRating
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CourseView> getAllPublishedCourses(
            String query,
            String categoryId,
            String instructorId,
            Integer rating,
            BigDecimal priceFrom,
            BigDecimal priceTo,
            Boolean hasCertificate,
            Boolean onSale,
            Long durationFrom,
            Long durationTo,
            int page, int size, String sort) {
        validatePublicSort(sort);
        PageQuery pageable = new PageQuery(page, size, sort);
        PageResult<Course> courses = courseRepository.searchPublished(query, categoryId, instructorId, rating, priceFrom, priceTo, hasCertificate, onSale, durationFrom, durationTo, pageable);
        return toResponse(courses).map(this::publicView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseView> getPublishedCoursesInOrder(List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        Map<String, Course> byId = courseRepository.findPublishedByIds(courseIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        return toResponse(courseIds.stream().map(byId::get).filter(Objects::nonNull).toList()).stream()
                .map(this::publicView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CourseView> getAllCourses(
            String query,
            String categoryId,
            String instructorId,
            Integer rating,
            BigDecimal priceFrom,
            BigDecimal priceTo,
            CourseStatus status,
            CoursePublishStatus publishStatus,
            int page, int size, String sort) {
        PageQuery pageable = new PageQuery(page, size, sort);
        PageResult<Course> courses = courseRepository.searchAll(query, categoryId, instructorId, rating, priceFrom, priceTo, null, null, status, publishStatus, pageable);
        return toResponse(courses);
    }

    @Transactional
    @Override
    public CourseView createCourse(String currentUserId, CreateCourseCommand request) {
        if (!Boolean.TRUE.equals(userService.isInstructorVerified(currentUserId).data())) {
            throw new ForbiddenException(ErrorCode.INSTRUCTOR_NOT_VERIFIED,
                    "An active verified instructor partnership is required");
        }
        Course course = courseRepository.save(buildCourseFromRequest(currentUserId, request));
        courseCollaborationService.initializeOwner(course.getId(), currentUserId);
        CourseDraft draft = courseDraftService.createDraftFromCourse(course);
        return toResponse(course, draft);
    }

    private Course buildCourseFromRequest(String currentUserId, CreateCourseCommand request) {
        Course course = courseMapper.toModel(request);
        course.setInstructorId(currentUserId);
        course.setEnrollmentCount(0L);
        course.setRating(null);
        course.setStatus(CourseStatus.DRAFT);
        course.setPublishStatus(null);
        course.setDiscountRate(calculateDiscountRate(request.price(), request.discountedPrice()));
        course.setCategoryId((category(request.categoryId())).getId());
        return course;
    }

    @Transactional
    @Override
    public CourseView updateCourse(String currentUserId, String courseId, UpdateCourseCommand request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new BadRequestException(ErrorCode.COURSE_ARCHIVED, "Archived courses cannot be updated");
        }
        Category category = category(request.categoryId());
        Long discountRate = calculateDiscountRate(request.price(), request.discountedPrice());
        CourseDraft draft = courseDraftService.updateDraft(course, request, category, discountRate);
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return toResponse(course, draft);
        }
        courseMapper.partialUpdate(course, request);
        course.setDiscountedPrice(request.discountedPrice());
        course.setCategoryId(category.getId());
        course.setDiscountRate(discountRate);
        course.setPublishStatus(null);
        if (Boolean.TRUE.equals(request.aiAssisted())) {
            course.setAiAssisted(true);
        }
        return toResponse(courseRepository.save(course), draft);
    }

    @Override
    @Transactional
    public CourseView submitCourse(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureCanSubmitCourse(currentUserId, course);
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new BadRequestException(ErrorCode.COURSE_ARCHIVED, "Archived courses cannot be submitted");
        }
        if (course.getPublishStatus() == CoursePublishStatus.WAITING_APPROVAL) {
            throw new BadRequestException(ErrorCode.COURSE_WAITING_APPROVAL, "Course is already waiting for approval");
        }
        Optional<CourseDraft> draft = courseDraftService.findDraft(course);
        if (draft.isEmpty()) {
            throw new BadRequestException(ErrorCode.COURSE_DRAFT_MISSING, "Course draft not found for course id: " + courseId);
        }
        CourseReadiness readiness = courseReadinessService.check(course);
        if (!readiness.ready()) {
            throw new BadRequestException(ErrorCode.COURSE_NOT_READY, "Course is not ready for review",
                    Map.of("errors", readiness.problems()));
        }
        course.setPublishStatus(CoursePublishStatus.WAITING_APPROVAL);
        course.setSubmittedAt(LocalDateTime.now(clock));
        Course savedCourse = courseRepository.save(course);
        // submitting again answers the last rejection's notes
        reviewNoteService.resolveSent(savedCourse.getId());
        courseEventProducer.publishCourseApprovalRequestedEvent(new CourseApprovalRequestedEvent(
                savedCourse.getId(),
                savedCourse.getTitle(),
                savedCourse.getInstructorId()
        ));
        return toResponse(savedCourse, draft.get());
    }

    @Override
    @Transactional
    public CourseView withdrawCourse(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureCanSubmitCourse(currentUserId, course);
        if (course.getPublishStatus() != CoursePublishStatus.WAITING_APPROVAL) {
            throw new BadRequestException(ErrorCode.COURSE_NOT_IN_REVIEW, "Only a course waiting for approval can be withdrawn");
        }
        // DRAFT goes back to a plain draft; PUBLISHED keeps selling and its draft becomes "changed" again
        course.setPublishStatus(null);
        course.setSubmittedAt(null);
        Course savedCourse = courseRepository.save(course);
        return courseDraftService.findDraft(savedCourse)
                .map(draft -> toResponse(savedCourse, draft))
                .orElseGet(() -> toResponse(savedCourse));
    }

    @Override
    @Transactional
    public CourseView archiveCourse(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureCanArchiveCourse(currentUserId, course);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BadRequestException(ErrorCode.COURSE_STATUS_INVALID, "Only published, non-archived courses can be archived");
        }
        course.setStatus(CourseStatus.ARCHIVED);
        course.setPublishStatus(null);
        course.setArchivedAt(LocalDateTime.now(clock));
        Course savedCourse = courseRepository.save(course);
        publishRecommendationEvent(savedCourse, "course.course.archived", "CourseArchived", false);
        publishSellability(savedCourse);
        publishTranscripts(savedCourse.getId(), CourseTranscriptsChanged.Reason.ARCHIVED);
        return toResponse(savedCourse);
    }

    @Override
    @Transactional
    public CourseView unarchiveCourse(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureCanArchiveCourse(currentUserId, course);
        if (course.getStatus() != CourseStatus.ARCHIVED) {
            throw new BadRequestException(ErrorCode.COURSE_STATUS_INVALID, "Only archived courses can be unarchived");
        }
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishStatus(null);
        course.setArchivedAt(null);
        Course savedCourse = courseRepository.save(course);
        publishRecommendationEvent(savedCourse, "course.course.published", "CoursePublished", true);
        publishSellability(savedCourse);
        publishTranscripts(savedCourse.getId(), CourseTranscriptsChanged.Reason.UNARCHIVED);
        return toResponse(savedCourse);
    }

    @Transactional
    @Override
    public CourseView approveCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new BadRequestException(ErrorCode.COURSE_ARCHIVED, "Archived courses cannot be approved");
        }
        if (course.getPublishStatus() != CoursePublishStatus.WAITING_APPROVAL) {
            throw new BadRequestException(ErrorCode.COURSE_STATUS_INVALID, "Only courses waiting for approval can be approved");
        }
        courseDraftService.approveDraft(course);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishStatus(CoursePublishStatus.PUBLISHED);
        if (course.getPublishedAt() == null) {
            course.setPublishedAt(LocalDateTime.now(clock));
        }
        course.setSubmittedAt(null);
        Course savedCourse = courseRepository.save(course);
        courseEventProducer.publishCourseContentPublishedEvent(new CourseContentPublishedEvent(
                savedCourse.getId(),
                savedCourse.getTitle(),
                savedCourse.getInstructorId(),
                courseCollaborationService.activeInstructorIds(savedCourse.getId())
        ));
        publishRecommendationEvent(savedCourse, "course.course.published", "CoursePublished", true);
        publishSellability(savedCourse);
        // approval swaps the published lessons for the draft's, so the transcript set changes with them
        publishTranscripts(savedCourse.getId(), CourseTranscriptsChanged.Reason.PUBLISHED);
        return toResponse(savedCourse);
    }

    private void publishTranscripts(String courseId, CourseTranscriptsChanged.Reason reason) {
        courseEventProducer.publishTranscriptsChanged(
                new CourseTranscriptsChanged(UUID.randomUUID().toString(), courseId, reason, clock.instant()));
    }

    @Transactional
    @Override
    public CourseView rejectCourse(String courseId, RejectCourseCommand request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new BadRequestException(ErrorCode.COURSE_ARCHIVED, "Archived courses cannot be rejected");
        }
        if (course.getPublishStatus() != CoursePublishStatus.WAITING_APPROVAL) {
            throw new BadRequestException(ErrorCode.COURSE_STATUS_INVALID, "Only courses waiting for approval can be rejected");
        }
        String reason = request == null || request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        List<String> noteIds = request == null || request.noteIds() == null ? List.of() : request.noteIds();
        if (reason == null && noteIds.isEmpty()) {
            throw new BadRequestException(ErrorCode.REVIEW_NOTES_OR_REASON_REQUIRED, "A rejection needs a reason or review notes");
        }
        CourseDraft draft = courseDraftService.findDraft(course).orElse(null);
        course.setPublishStatus(CoursePublishStatus.REJECTED);
        course.setSubmittedAt(null);
        reviewNoteService.send(courseId, noteIds);
        RejectCourseReason rejectReason = rejectCourseReasonRepository.findByCourse(courseId)
                .orElseGet(() -> RejectCourseReason.builder()
                        .courseId(courseId)
                        .build());
        rejectReason.setReason(reason);
        rejectCourseReasonRepository.save(rejectReason);
        return toResponse(courseRepository.save(course), draft);
    }

    @Override
    public RejectCourseView getRejectReason(String currentUserId, String courseId) {
        requireCourseTeamOrAdmin(currentUserId, courseId);
        RejectCourseReason reason = rejectCourseReasonRepository.findByCourse(courseId)
                .orElseThrow(() -> new NotFoundException("Reject reason not found for course id: " + courseId));
        LocalDateTime rejectedAt = reason.getUpdatedAt() != null ? reason.getUpdatedAt() : reason.getCreatedAt();
        return new RejectCourseView(reason.getCourseId(), reason.getReason(), rejectedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewNoteView.Rejection getReviewNotes(String currentUserId, String courseId) {
        requireCourseTeamOrAdmin(currentUserId, courseId);
        String reason = rejectCourseReasonRepository.findByCourse(courseId).map(RejectCourseReason::getReason).orElse(null);
        return new ReviewNoteView.Rejection(reason, reviewNoteService.sent(courseId));
    }

    /** Review feedback is for the course's own team (any active role) and reviewers, not every signed-in user. */
    private void requireCourseTeamOrAdmin(String currentUserId, String courseId) {
        if (courseAccessService.isAdmin()) {
            return;
        }
        if (currentUserId == null || !courseAccessService.hasCourseCapability(currentUserId, courseId, CourseMembershipRole.EDITOR)) {
            throw new ForbiddenException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED, "Only the course's instructors can read its review feedback");
        }
    }

    @Override
    @Transactional
    public void updateCourseRating(String courseId, Double rating) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        course.setRating(rating);
        Course savedCourse = courseRepository.save(course);
        publishRecommendationEvent(savedCourse, "course.course.updated", "CourseUpdated", false);
    }

    @Override
    @Transactional
    public void updateCourseRatingStats(String courseId, Double rating, long ratingCount) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        course.setRating(rating);
        course.setRatingCount(ratingCount);
        Course savedCourse = courseRepository.save(course);
        publishRecommendationEvent(savedCourse, "course.course.updated", "CourseUpdated", false);
    }

    @Override
    @Transactional
    public void increaseEnrollmentCount(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        course.setEnrollmentCount(course.getEnrollmentCount() == null ? 1L : course.getEnrollmentCount() + 1);
        Course savedCourse = courseRepository.save(course);
        publishRecommendationEvent(savedCourse, "course.course.updated", "CourseUpdated", false);
    }

    @Override
    @Transactional
    public void replayRecommendationEvents() {
        courseRepository.findAll().stream()
                .filter(course -> course.getStatus() == CourseStatus.PUBLISHED)
                .forEach(course -> publishRecommendationEvent(course, "course.course.updated", "CourseUpdated", true));
    }

    private CourseView toResponse(Course course) {
        UserView instructor = userService.getInstructorById(course.getInstructorId()).data();
        return courseMapper.toResponse(course, categoryOrNull(course.getCategoryId()), instructor);
    }

    private CourseView toResponse(Course course, CourseDraft draft) {
        if (draft != null) {
            UserView instructor = userService.getInstructorById(course.getInstructorId()).data();
            return courseMapper.toResponse(course, draft, categoryOrNull(draft.getCategoryId()), instructor);
        }
        return toResponse(course);
    }

    private CourseView draftOnlyResponse(Course course) {
        CourseDraft draft = courseDraftService.findDraft(course).orElse(null);
        if (draft != null) {
            return toResponse(course, draft);
        }
        if (course.getStatus() == CourseStatus.DRAFT) {
            return toResponse(course);
        }
        throw new NotFoundException("Course draft not found for course id: " + course.getId());
    }

    private PageResult<CourseView> toResponse(PageResult<Course> courses) {
        Map<String, UserView> instructorMap = userService.getInstructorsByIds(courses.content().stream()
                .map(Course::getInstructorId)
                .toList()).data()
                .stream().collect(Collectors.toMap(UserView::userId, Function.identity()));
        Map<String, Category> categoryMap = categoriesOf(courses.content());
        return courses.map(course -> courseMapper.toResponse(course, categoryMap.get(course.getCategoryId()), instructorMap.get(course.getInstructorId())));
    }

    private List<CourseView> toResponse(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }
        Map<String, UserView> instructorMap = userService.getInstructorsByIds(courses.stream()
                .map(Course::getInstructorId)
                .toList()).data()
                .stream().collect(Collectors.toMap(UserView::userId, Function.identity()));
        Map<String, Category> categoryMap = categoriesOf(courses);
        return courses.stream()
                .map(course -> courseMapper.toResponse(course, categoryMap.get(course.getCategoryId()), instructorMap.get(course.getInstructorId())))
                .toList();
    }

    private Category categoryOrNull(String categoryId) {
        return categoryId == null ? null : categoryRepository.findById(categoryId).orElse(null);
    }

    private Map<String, Category> categoriesOf(List<Course> courses) {
        List<String> ids = courses.stream().map(Course::getCategoryId).filter(Objects::nonNull).distinct().toList();
        return categoryRepository.findAllById(ids).stream().collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private Category category(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));
    }

    private Long calculateDiscountRate(BigDecimal price, BigDecimal discountedPrice) {
        if (price == null || price.signum() == 0 || discountedPrice == null) {
            return 0L;
        }
        return price.subtract(discountedPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(price, 0, RoundingMode.DOWN)
                .longValueExact();
    }

    /** Fields the public catalog may sort by; anything else is a client error, not a silent reorder. */
    private static final Set<String> PUBLIC_SORT_FIELDS =
            Set.of("rating", "discountedPrice", "createdAt", "enrollmentCount");

    private void validatePublicSort(String sort) {
        SortConverter.properties(sort).forEach(property -> {
            if (!PUBLIC_SORT_FIELDS.contains(property)) {
                throw new BadRequestException(ErrorCode.INVALID_SORT,
                        "Unsupported sort field: " + property + ". Allowed: " + PUBLIC_SORT_FIELDS);
            }
        });
    }

    private void publishRecommendationEvent(Course course, String routingKey, String eventType, boolean includeCurriculum) {
        CourseView response = toResponse(course);
        CourseRecommendationPayload payload = new CourseRecommendationPayload(
                response.id(),
                response.title(),
                response.description(),
                response.category(),
                response.instructor() == null ? null : response.instructor().withoutBio(),
                response.images(),
                response.price(),
                response.discountedPrice(),
                response.discountRate(),
                response.rating(),
                response.enrollmentCount(),
                response.isInSubscription(),
                course.getAccessModel() == null ? null : course.getAccessModel().name(),
                course.getSubscriptionFeatureKey(),
                response.duration(),
                response.hasCertificate(),
                response.certificateTitle(),
                response.status(),
                response.publishStatus(),
                includeCurriculum ? curriculumService.getEnrolledCurriculum(course.getId()).sections() : null,
                response.createdAt(),
                response.updatedAt()
        );
        courseEventProducer.publishCourseRecommendationEvent(
                routingKey,
                eventType,
                new CourseRecommendationEvent(payload, LocalDateTime.now())
        );
    }

}

