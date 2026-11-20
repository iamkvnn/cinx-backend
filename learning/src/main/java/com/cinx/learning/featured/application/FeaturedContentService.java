package com.cinx.learning.featured.application;

import com.cinx.learning.community.application.CommunityLookup;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.enrollment.application.EnrollmentAccess;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.application.query.CourseView;
import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.catalog.application.query.CourseImageInfo;
import com.cinx.learning.catalog.application.ICourseService;
import com.cinx.learning.community.application.UserSummaryDirectory;
import com.cinx.learning.community.application.query.UserSummaryView;
import com.cinx.learning.community.application.query.ReviewInfo;
import com.cinx.learning.featured.application.command.AddFeaturedItemCommand;
import com.cinx.learning.featured.application.command.ReorderCommand;
import com.cinx.learning.featured.application.command.UpdateFeaturedItemCommand;
import com.cinx.learning.featured.application.command.UpsertFeaturedPathCommand;
import com.cinx.learning.featured.application.query.FeaturedCourseView;
import com.cinx.learning.featured.application.query.FeaturedPathDetailView;
import com.cinx.learning.featured.application.query.FeaturedPathView;
import com.cinx.learning.featured.application.query.FeaturedReviewView;
import com.cinx.learning.featured.application.query.PublicStatsView;
import com.cinx.learning.featured.domain.FeaturedCourse;
import com.cinx.learning.featured.domain.FeaturedLearningPath;
import com.cinx.learning.featured.domain.FeaturedReview;
import com.cinx.learning.featured.domain.FeaturedCourseRepository;
import com.cinx.learning.featured.domain.FeaturedLearningPathRepository;
import com.cinx.learning.featured.domain.FeaturedReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeaturedContentService implements IFeaturedContentService {
    private static final Duration STATS_TTL = Duration.ofMinutes(10);
    private final CommunityLookup communityLookup;
    private final CatalogLookup catalogLookup;
    private final EnrollmentAccess enrollmentAccess;
    private final FeaturedLearningPathRepository pathRepository;
    private final FeaturedCourseRepository featuredCourseRepository;
    private final FeaturedReviewRepository featuredReviewRepository;
    private final ICourseService courseService;
    private final UserSummaryDirectory userClient;
    private final Clock clock;

    private volatile PublicStatsView cachedStats;
    private volatile Instant cachedStatsAt;

    // ---------------------------------------------------------------- public

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedPathView> getPublicPaths() {
        List<FeaturedLearningPath> paths = pathRepository.findByActiveTrueOrderBySortOrderAsc();
        Map<String, CourseInfo> published = publishedCoursesById(paths.stream()
                .flatMap(path -> path.getCourseIds().stream())
                .toList());
        return paths.stream()
                .map(path -> toPathResponse(path, published))
                .filter(path -> !path.courses().isEmpty())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FeaturedPathDetailView getPublicPath(String id) {
        FeaturedLearningPath path = pathRepository.findById(id)
                .filter(FeaturedLearningPath::getActive)
                .orElseThrow(() -> new NotFoundException("Featured path not found with id: " + id));
        List<CourseView> courses = courseService.getPublishedCoursesInOrder(path.getCourseIds());
        if (courses.isEmpty()) {
            throw new NotFoundException("Featured path has no published course: " + id);
        }
        return new FeaturedPathDetailView(path.getId(), path.getName(), path.getRoleTitle(),
                path.getEstimatedMonths(), path.getHoursPerWeek(), path.getDescription(),
                List.copyOf(path.getOutcomes()), courses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedCourseView> getPublicCourses() {
        List<FeaturedCourse> featured = featuredCourseRepository.findByActiveTrueOrderBySortOrderAsc();
        Map<String, CourseView> courses = courseService.getPublishedCoursesInOrder(featured.stream()
                        .map(FeaturedCourse::getCourseId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(CourseView::id, Function.identity(), (a, b) -> a));
        return featured.stream()
                .filter(item -> courses.containsKey(item.getCourseId()))
                .map(item -> new FeaturedCourseView(item.getId(), item.getSortOrder(), item.getActive(), courses.get(item.getCourseId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedReviewView> getPublicReviews() {
        return toReviewResponses(featuredReviewRepository.findByActiveTrueOrderBySortOrderAsc(), true);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicStatsView getPublicStats() {
        Instant now = clock.instant();
        PublicStatsView cached = cachedStats;
        Instant cachedAt = cachedStatsAt;
        if (cached != null && cachedAt != null && cachedAt.plus(STATS_TTL).isAfter(now)) {
            return cached;
        }
        PublicStatsView fresh = new PublicStatsView(
                enrollmentAccess.countDistinctLearners(),
                catalogLookup.countInstructorsOfPublishedCourses(),
                catalogLookup.countCoursesByStatus(CourseStatus.PUBLISHED));
        cachedStats = fresh;
        cachedStatsAt = now;
        return fresh;
    }

    // ------------------------------------------------------------ admin: paths

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedPathView> getAllPaths() {
        List<FeaturedLearningPath> paths = pathRepository.findAllByOrderBySortOrderAsc();
        Map<String, CourseInfo> published = publishedCoursesById(paths.stream()
                .flatMap(path -> path.getCourseIds().stream())
                .toList());
        return paths.stream().map(path -> toPathResponse(path, published)).toList();
    }

    @Override
    @Transactional
    public FeaturedPathView createPath(UpsertFeaturedPathCommand request) {
        List<String> courseIds = validatedPathCourses(request.courseIds());
        FeaturedLearningPath path = FeaturedLearningPath.builder()
                .name(request.name().trim())
                .roleTitle(request.roleTitle().trim())
                .estimatedMonths(request.estimatedMonths())
                .hoursPerWeek(request.hoursPerWeek())
                .active(request.active() == null || request.active())
                .sortOrder(pathRepository.maxSortOrder() + 1)
                .courseIds(new ArrayList<>(courseIds))
                .description(trimToNull(request.description()))
                .outcomes(new ArrayList<>(cleanOutcomes(request.outcomes())))
                .build();
        FeaturedLearningPath saved = pathRepository.save(path);
        return toPathResponse(saved, publishedCoursesById(courseIds));
    }

    @Override
    @Transactional
    public FeaturedPathView updatePath(String id, UpsertFeaturedPathCommand request) {
        FeaturedLearningPath path = pathRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Featured path not found with id: " + id));
        List<String> courseIds = validatedPathCourses(request.courseIds());
        path.setName(request.name().trim());
        path.setRoleTitle(request.roleTitle().trim());
        path.setEstimatedMonths(request.estimatedMonths());
        path.setHoursPerWeek(request.hoursPerWeek());
        if (request.active() != null) {
            path.setActive(request.active());
        }
        path.getCourseIds().clear();
        path.getCourseIds().addAll(courseIds);
        path.setDescription(trimToNull(request.description()));
        path.getOutcomes().clear();
        path.getOutcomes().addAll(cleanOutcomes(request.outcomes()));
        FeaturedLearningPath saved = pathRepository.save(path);
        return toPathResponse(saved, publishedCoursesById(courseIds));
    }

    @Override
    @Transactional
    public void deletePath(String id) {
        FeaturedLearningPath path = pathRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Featured path not found with id: " + id));
        pathRepository.delete(path);
    }

    @Override
    @Transactional
    public List<FeaturedPathView> reorderPaths(ReorderCommand request) {
        List<FeaturedLearningPath> all = pathRepository.findAll();
        applyOrder(all, FeaturedLearningPath::getId, FeaturedLearningPath::setSortOrder, request.ids());
        pathRepository.saveAll(all);
        return getAllPaths();
    }

    // ---------------------------------------------------------- admin: courses

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedCourseView> getAllCourses() {
        List<FeaturedCourse> featured = featuredCourseRepository.findAllByOrderBySortOrderAsc();
        Map<String, CourseView> courses = courseService.getPublishedCoursesInOrder(featured.stream()
                        .map(FeaturedCourse::getCourseId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(CourseView::id, Function.identity(), (a, b) -> a));
        // an entry whose course is no longer published is returned with course = null, so the admin can remove it
        return featured.stream()
                .map(item -> new FeaturedCourseView(item.getId(), item.getSortOrder(), item.getActive(), courses.get(item.getCourseId())))
                .toList();
    }

    @Override
    @Transactional
    public FeaturedCourseView addCourse(AddFeaturedItemCommand request) {
        String courseId = request.targetId().trim();
        if (featuredCourseRepository.existsByCourseId(courseId)) {
            throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "CourseInfo is already featured");
        }
        List<CourseView> course = courseService.getPublishedCoursesInOrder(List.of(courseId));
        if (course.isEmpty()) {
            throw new BadRequestException(ErrorCode.COURSE_STATUS_INVALID, "Only published courses can be featured");
        }
        FeaturedCourse saved = featuredCourseRepository.save(FeaturedCourse.builder()
                .courseId(courseId)
                .sortOrder(featuredCourseRepository.maxSortOrder() + 1)
                .active(true)
                .build());
        return new FeaturedCourseView(saved.getId(), saved.getSortOrder(), saved.getActive(), course.get(0));
    }

    @Override
    @Transactional
    public FeaturedCourseView updateCourse(String id, UpdateFeaturedItemCommand request) {
        FeaturedCourse item = featuredCourseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Featured course not found with id: " + id));
        item.setActive(request.active());
        FeaturedCourse saved = featuredCourseRepository.save(item);
        List<CourseView> course = courseService.getPublishedCoursesInOrder(List.of(saved.getCourseId()));
        return new FeaturedCourseView(saved.getId(), saved.getSortOrder(), saved.getActive(), course.isEmpty() ? null : course.get(0));
    }

    @Override
    @Transactional
    public void removeCourse(String id) {
        FeaturedCourse item = featuredCourseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Featured course not found with id: " + id));
        featuredCourseRepository.delete(item);
    }

    @Override
    @Transactional
    public List<FeaturedCourseView> reorderCourses(ReorderCommand request) {
        List<FeaturedCourse> all = featuredCourseRepository.findAll();
        applyOrder(all, FeaturedCourse::getId, FeaturedCourse::setSortOrder, request.ids());
        featuredCourseRepository.saveAll(all);
        return getAllCourses();
    }

    // ---------------------------------------------------------- admin: reviews

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedReviewView> getAllReviews() {
        return toReviewResponses(featuredReviewRepository.findAllByOrderBySortOrderAsc(), false);
    }

    @Override
    @Transactional
    public FeaturedReviewView addReview(AddFeaturedItemCommand request) {
        String reviewId = request.targetId().trim();
        if (featuredReviewRepository.existsByReviewId(reviewId)) {
            throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "ReviewInfo is already featured");
        }
        if (!communityLookup.reviewExists(reviewId)) {
            throw new NotFoundException("ReviewInfo not found with id: " + reviewId);
        }
        FeaturedReview saved = featuredReviewRepository.save(FeaturedReview.builder()
                .reviewId(reviewId)
                .sortOrder(featuredReviewRepository.maxSortOrder() + 1)
                .active(true)
                .build());
        return toReviewResponses(List.of(saved), false).get(0);
    }

    @Override
    @Transactional
    public FeaturedReviewView updateReview(String id, UpdateFeaturedItemCommand request) {
        FeaturedReview item = featuredReviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Featured review not found with id: " + id));
        item.setActive(request.active());
        return toReviewResponses(List.of(featuredReviewRepository.save(item)), false).get(0);
    }

    @Override
    @Transactional
    public void removeReview(String id) {
        FeaturedReview item = featuredReviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Featured review not found with id: " + id));
        featuredReviewRepository.delete(item);
    }

    @Override
    @Transactional
    public List<FeaturedReviewView> reorderReviews(ReorderCommand request) {
        List<FeaturedReview> all = featuredReviewRepository.findAll();
        applyOrder(all, FeaturedReview::getId, FeaturedReview::setSortOrder, request.ids());
        featuredReviewRepository.saveAll(all);
        return getAllReviews();
    }

    // ---------------------------------------------------------------- helpers

    private List<String> validatedPathCourses(List<String> requested) {
        List<String> courseIds = requested.stream().map(String::trim).toList();
        if (new HashSet<>(courseIds).size() != courseIds.size()) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "A course can appear only once in a featured path");
        }
        Set<String> published = catalogLookup.publishedCourses(courseIds).stream()
                .map(CourseInfo::getId)
                .collect(Collectors.toSet());
        List<String> invalid = courseIds.stream().filter(id -> !published.contains(id)).toList();
        if (!invalid.isEmpty()) {
            throw new BadRequestException(ErrorCode.COURSE_STATUS_INVALID,
                    "Featured paths may only contain published courses; not published or unknown: " + invalid);
        }
        return courseIds;
    }

    private Map<String, CourseInfo> publishedCoursesById(Collection<String> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return catalogLookup.publishedCourses(courseIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(CourseInfo::getId, Function.identity()));
    }

    private FeaturedPathView toPathResponse(FeaturedLearningPath path, Map<String, CourseInfo> published) {
        List<FeaturedPathView.Course> courses = path.getCourseIds().stream()
                .map(published::get)
                .filter(Objects::nonNull)
                .map(course -> new FeaturedPathView.Course(course.getId(), course.getTitle(), thumbnail(course)))
                .toList();
        return new FeaturedPathView(path.getId(), path.getName(), path.getRoleTitle(), path.getEstimatedMonths(),
                path.getHoursPerWeek(), path.getSortOrder(), path.getActive(), courses,
                path.getDescription(), List.copyOf(path.getOutcomes()));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<String> cleanOutcomes(List<String> outcomes) {
        return outcomes == null ? List.of() : outcomes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private static String thumbnail(CourseInfo course) {
        return course.getImages() == null ? null : course.getImages().stream()
                .map(CourseImageInfo::getImageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<FeaturedReviewView> toReviewResponses(List<FeaturedReview> featured, boolean skipMissing) {
        if (featured.isEmpty()) {
            return List.of();
        }
        Map<String, ReviewInfo> reviews = communityLookup.reviews(featured.stream().map(FeaturedReview::getReviewId).toList())
                .stream()
                .collect(Collectors.toMap(ReviewInfo::getId, Function.identity()));
        Map<String, CourseInfo> courses = catalogLookup.courses(reviews.values().stream()
                        .map(ReviewInfo::getCourseId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(CourseInfo::getId, Function.identity()));
        Map<String, UserSummaryView> users = reviewersById(reviews.values());
        List<FeaturedReviewView> out = new ArrayList<>();
        for (FeaturedReview item : featured) {
            ReviewInfo review = reviews.get(item.getReviewId());
            CourseInfo course = review == null ? null : courses.get(review.getCourseId());
            boolean publishable = review != null && course != null && course.getStatus() == CourseStatus.PUBLISHED;
            if (skipMissing && !publishable) {
                continue;
            }
            UserSummaryView user = review == null ? null : users.get(review.getUserId());
            out.add(new FeaturedReviewView(
                    item.getId(),
                    item.getSortOrder(),
                    item.getActive(),
                    item.getReviewId(),
                    review == null ? null : review.getRating(),
                    review == null ? null : review.getContent(),
                    user == null ? null : new FeaturedReviewView.Reviewer(user.name(), user.avatarUrl()),
                    course == null ? null : new FeaturedReviewView.CourseRef(course.getId(), course.getTitle())));
        }
        return out;
    }

    /** Best effort, like ReviewService: a user-service failure hides names, it does not fail the page. */
    private Map<String, UserSummaryView> reviewersById(Collection<ReviewInfo> reviews) {
        LinkedHashSet<String> ids = reviews.stream()
                .map(ReviewInfo::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            ApiResponse<List<UserSummaryView>> response = userClient.getUsersByIds(List.copyOf(ids));
            if (response == null || !response.success() || response.data() == null) {
                return Map.of();
            }
            return response.data().stream()
                    .filter(user -> user.userId() != null)
                    .collect(Collectors.toMap(UserSummaryView::userId, Function.identity(), (a, b) -> a));
        } catch (Exception ex) {
            log.warn("Failed to fetch featured review user summaries", ex);
            return Map.of();
        }
    }

    /** Reorder must name every existing item exactly once, so no item is silently dropped from the order. */
    private static <T> void applyOrder(List<T> items, Function<T, String> id,
                                       BiConsumer<T, Integer> setOrder, List<String> orderedIds) {
        Set<String> existing = items.stream().map(id).collect(Collectors.toSet());
        if (orderedIds.size() != existing.size() || !existing.equals(new HashSet<>(orderedIds))) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED,
                    "Reorder must list every item exactly once");
        }
        Map<String, T> byId = items.stream().collect(Collectors.toMap(id, Function.identity()));
        for (int i = 0; i < orderedIds.size(); i++) {
            setOrder.accept(byId.get(orderedIds.get(i)), i);
        }
        items.sort(Comparator.comparing(item -> orderedIds.indexOf(id.apply(item))));
    }
}
