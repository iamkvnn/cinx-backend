package com.cinx.learning.learningpath.application;

import com.cinx.learning.progress.application.ProgressLookup;
import com.cinx.learning.progress.application.query.CourseProgressInfo;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LearningPathStatus;
import com.cinx.learning.learningpath.application.command.LearningPathItemCommand;
import com.cinx.learning.learningpath.application.command.LearningPathItemsUpdateCommand;
import com.cinx.learning.learningpath.application.command.LearningPathCommand;
import com.cinx.learning.enrollment.application.query.CheckEnrollmentStatus;
import com.cinx.learning.learningpath.application.query.LearningPathView;
import com.cinx.learning.learningpath.application.mapper.LearningPathMapper;
import com.cinx.learning.progress.application.query.LearningItemProgressInfo;
import com.cinx.learning.learningpath.domain.LearningPathItem;
import com.cinx.learning.learningpath.domain.UserLearningPath;
import com.cinx.learning.learningpath.domain.LearningPathItemRepository;
import com.cinx.learning.learningpath.domain.UserLearningPathRepository;
import com.cinx.learning.enrollment.application.EnrollmentAccess;
import com.cinx.learning.studyplan.application.IStudyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningPathService implements ILearningPathService {
    private static final List<LearningPathStatus> OPEN = List.of(LearningPathStatus.ACTIVE, LearningPathStatus.PENDING_PAYMENT);
    private static final String ONE_ACTIVE_KEY = "uk_learning_path_one_active";
    private static final String ALREADY_ACTIVE_MESSAGE = "You already have an active or pending learning path. Drop or complete it to create a new one.";
    private final ProgressLookup progressLookup;
    private final UserLearningPathRepository pathRepository;
    private final LearningPathItemRepository itemRepository;
    private final LearningPathMapper pathMapper;
    private final EnrollmentAccess enrollmentAccess;
    private final LearningPathCheckouts checkouts;
    private final LearningPathItemEnricher itemEnricher;
    private final IStudyPlanService studyPlanService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<LearningPathView> getLearningPaths(String userId) {
        return pathRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(UserLearningPath::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(pathMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPathView getLearningPath(String userId, String id) {
        return enriched(userId, ownedPath(userId, id));
    }

    @Transactional
    @Override
    public LearningPathView createLearningPath(String userId, String idempotencyKey, LearningPathCommand request) {
        String key = normalizeIdempotencyKey(idempotencyKey);
        String fingerprint = key == null ? null : fingerprint(request);
        if (key != null) {
            // A retry of a create whose response was lost gets the same path back.
            Optional<UserLearningPath> previous = pathRepository.findByUserIdAndIdempotencyKey(userId, key);
            if (previous.isPresent()) {
                if (!fingerprint.equals(previous.get().getRequestFingerprint()))
                    throw new BadRequestException("Idempotency-Key was already used with another request");
                return enriched(userId, previous.get());
            }
        }

        pathRepository.findByUserIdAndStatusIn(userId, OPEN)
                .ifPresent(p -> {
                    throw new ConflictException(ErrorCode.LEARNING_PATH_ALREADY_ACTIVE, ALREADY_ACTIVE_MESSAGE);
                });

        List<LearningPathItemCommand> requested = request.getItems();
        List<String> distinctCourseIds = distinctCourseIds(requested);
        Set<String> entitledCourseIds = entitledCourseIds(userId, distinctCourseIds);
        List<String> unenrolledCourseIds = distinctCourseIds.stream()
                .filter(courseId -> !entitledCourseIds.contains(courseId))
                .toList();
        Set<String> completedLessonIds = completedLessonIds(userId, requested);

        UserLearningPath path = UserLearningPath.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .idempotencyKey(key)
                .requestFingerprint(fingerprint)
                .createdAt(LocalDateTime.now(clock))
                .items(new ArrayList<>())
                .build();
        replaceItemsOf(path, requested, completedLessonIds);
        applyStatus(path, !unenrolledCourseIds.isEmpty());

        UserLearningPath savedPath;
        try {
            // flush now, so a concurrent create caught by the unique keys fails here with a 409
            savedPath = pathRepository.saveAndFlush(path);
        } catch (DataIntegrityViolationException e) {
            throw conflictOf(e);
        }

        if (savedPath.getStatus() == LearningPathStatus.PENDING_PAYMENT) {
            for (String cId : unenrolledCourseIds) {
                checkouts.requestCheckout(userId, cId);
            }
        }
        if (OPEN.contains(savedPath.getStatus())) {
            studyPlanService.attachPath(userId, savedPath.getId());
        }
        return enriched(userId, savedPath, entitledCourseIds);
    }

    @Override
    public LearningPathView replaceItems(String userId, String id, LearningPathItemsUpdateCommand request) {
        UserLearningPath path = ownedPath(userId, id);
        if (!OPEN.contains(path.getStatus())) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "Only an active or pending learning path can be edited");
        }
        Set<String> previousCourseIds = path.getItems().stream().map(LearningPathItem::getCourseId).collect(Collectors.toSet());

        List<LearningPathItemCommand> requested = request.getItems();
        List<String> distinctCourseIds = distinctCourseIds(requested);
        Set<String> entitledCourseIds = entitledCourseIds(userId, distinctCourseIds);
        List<String> unenrolledCourseIds = distinctCourseIds.stream()
                .filter(courseId -> !entitledCourseIds.contains(courseId))
                .toList();

        if (request.getTitle() != null && !request.getTitle().isBlank()) path.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) path.setDescription(request.getDescription());
        replaceItemsOf(path, requested, completedLessonIds(userId, requested));
        applyStatus(path, !unenrolledCourseIds.isEmpty());
        UserLearningPath saved = pathRepository.save(path);

        // the cart already holds the courses that were unpaid before; only a course new to the path is added.
        // A retried PUT finds those courses in the path already, so it queues nothing twice.
        for (String cId : unenrolledCourseIds) {
            if (!previousCourseIds.contains(cId)) {
                checkouts.requestCheckout(userId, cId);
            }
        }
        if (OPEN.contains(saved.getStatus())) {
            studyPlanService.attachPath(userId, saved.getId());
        }
        return enriched(userId, saved, entitledCourseIds);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return null;
        String key = idempotencyKey.trim();
        if (key.length() > 255) throw new BadRequestException("Idempotency-Key must be at most 255 characters");
        return key;
    }

    private String fingerprint(LearningPathCommand request) {
        StringBuilder payload = new StringBuilder()
                .append(request.getTitle()).append('\n')
                .append(request.getDescription()).append('\n');
        for (LearningPathItemCommand item : request.getItems()) {
            payload.append(item.getCourseId()).append('|')
                    .append(item.getLessonId()).append('|')
                    .append(item.getOrderIndex()).append('|')
                    .append(item.getIsSuggested()).append('|')
                    .append(phaseOf(item)).append('\n');
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPathView getActiveLearningPath(String userId) {
        UserLearningPath path = pathRepository.findByUserIdAndStatusIn(userId, OPEN)
                .orElseThrow(() -> new NotFoundException("No active learning path found for user"));
        return pathMapper.toDto(path);
    }

    @Override
    public void dropActiveLearningPath(String userId) {
        UserLearningPath path = pathRepository.findByUserIdAndStatusIn(userId, OPEN)
                .orElseThrow(() -> new NotFoundException("No active learning path found for user"));

        path.setStatus(LearningPathStatus.DROPPED);
        path.setDroppedAt(LocalDateTime.now(clock));
        pathRepository.save(path);
    }

    @Override
    public void activatePendingPathForCourse(String userId, String courseId) {
        Optional<UserLearningPath> optPath = pathRepository.findByUserIdAndStatus(userId, LearningPathStatus.PENDING_PAYMENT);
        if (optPath.isEmpty()) return;

        UserLearningPath path = optPath.get();
        // Check if any other courses in this path are still unenrolled
        List<String> courseIdsInPath = path.getItems().stream()
                .map(LearningPathItem::getCourseId)
                .distinct()
                .toList();

        long enrolledCourseCount = progressLookup.courseProgressOfUser(userId, courseIdsInPath).stream()
                .map(CourseProgressInfo::getCourseId)
                .distinct()
                .count();
        boolean allEnrolled = enrolledCourseCount == courseIdsInPath.size();

        if (allEnrolled) {
            // lessons finished while the path waited for payment count now
            Set<String> completed = completedOf(userId, path.getItems().stream().map(LearningPathItem::getLessonId).toList());
            path.getItems().forEach(item -> item.setIsCompleted(completed.contains(item.getLessonId())));
            recount(path);
            applyStatus(path, false);
            pathRepository.save(path);
        }
    }

    @Override
    public void updatePathProgress(String userId, String lessonId) {
        // a pending path counts progress too: its owned courses are studied while the rest wait for payment
        Optional<UserLearningPath> optPath = pathRepository.findByUserIdAndStatusIn(userId, OPEN);
        if (optPath.isEmpty()) return;

        UserLearningPath path = optPath.get();
        Optional<LearningPathItem> optItem = itemRepository.findByLearningPathIdAndLessonId(path.getId(), lessonId);
        if (optItem.isPresent() && !optItem.get().getIsCompleted()) {
            LearningPathItem item = optItem.get();
            item.setIsCompleted(true);
            itemRepository.save(item);

            path.setCompletedItems(path.getCompletedItems() + 1);
            double progress = (double) path.getCompletedItems() / path.getTotalItems() * 100;
            path.setCurrentProgress(progress);

            if (path.getStatus() == LearningPathStatus.ACTIVE && path.getCompletedItems().equals(path.getTotalItems())) {
                path.setStatus(LearningPathStatus.COMPLETED);
                path.setCompletedAt(LocalDateTime.now(clock));
            }
            pathRepository.save(path);
        }
    }

    @Override
    public void refreshPrerequisiteUnlocks(String userId, String courseId) {
        // Normalise the whole path's order to 0..n-1, keeping the stored relative order. Re-indexing one course from 0
        // used to give items of different courses the same index.
        pathRepository.findByUserIdAndStatusIn(userId, OPEN)
                .filter(path -> path.getItems().stream().anyMatch(item -> courseId.equals(item.getCourseId())))
                .ifPresent(path -> {
                    List<LearningPathItem> ordered = path.getItems().stream()
                            .sorted(Comparator.comparing(LearningPathItem::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                            .toList();
                    boolean changed = false;
                    int position = 0;
                    for (LearningPathItem item : ordered) {
                        if (item.getOrderIndex() == null || item.getOrderIndex() != position) {
                            item.setOrderIndex(position);
                            changed = true;
                        }
                        position++;
                    }
                    if (changed) {
                        itemRepository.saveAll(ordered);
                    }
                });
    }

    // ----------------------------------------------------------------- helpers

    private UserLearningPath ownedPath(String userId, String id) {
        return pathRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Learning path not found"));
    }

    private LearningPathView enriched(String userId, UserLearningPath path) {
        return enriched(userId, path, entitledCourseIds(userId, path.getItems().stream().map(LearningPathItem::getCourseId).distinct().toList()));
    }

    private LearningPathView enriched(String userId, UserLearningPath path, Set<String> entitledCourseIds) {
        return itemEnricher.enrich(userId, pathMapper.toDto(path), entitledCourseIds);
    }

    private static List<String> distinctCourseIds(List<LearningPathItemCommand> items) {
        return items.stream().map(LearningPathItemCommand::getCourseId).distinct().toList();
    }

    /** The local entitlement projection, with the enrollment service as a fallback for courses it does not know yet. */
    private Set<String> entitledCourseIds(String userId, List<String> courseIds) {
        if (courseIds.isEmpty()) return new HashSet<>();
        Set<String> entitled = new HashSet<>(enrollmentAccess.entitledCourseIds(userId, courseIds));
        List<String> missing = courseIds.stream().filter(courseId -> !entitled.contains(courseId)).toList();
        if (!missing.isEmpty()) {
            ApiResponse<List<CheckEnrollmentStatus>> checkRes = enrollmentAccess.checkEnrollmentStatus(userId, missing);
            checkRes.data().stream()
                    .filter(CheckEnrollmentStatus::isEnrolled)
                    .map(CheckEnrollmentStatus::courseId)
                    .forEach(entitled::add);
        }
        return entitled;
    }

    private Set<String> completedLessonIds(String userId, List<LearningPathItemCommand> items) {
        return completedOf(userId, items.stream().map(LearningPathItemCommand::getLessonId).toList());
    }

    private Set<String> completedOf(String userId, List<String> lessonIds) {
        if (lessonIds.isEmpty()) return Set.of();
        return progressLookup.itemProgressOfUser(userId, lessonIds).stream()
                .filter(progress -> Boolean.TRUE.equals(progress.getIsCompleted()))
                .map(LearningItemProgressInfo::getItemId)
                .collect(Collectors.toSet());
    }

    /** The server owns the order: the list order of the request, 0..n-1. A lesson listed twice is kept once. */
    private static void replaceItemsOf(UserLearningPath path, List<LearningPathItemCommand> requested, Set<String> completedLessonIds) {
        Set<String> seen = new HashSet<>();
        List<LearningPathItem> items = new ArrayList<>();
        for (LearningPathItemCommand req : requested) {
            if (!seen.add(req.getCourseId() + '|' + req.getLessonId())) continue;
            items.add(LearningPathItem.builder()
                    .learningPathId(path.getId())
                    .courseId(req.getCourseId())
                    .lessonId(req.getLessonId())
                    .orderIndex(items.size())
                    .phase(phaseOf(req))
                    .isSuggested(Boolean.TRUE.equals(req.getIsSuggested()))
                    .isCompleted(completedLessonIds.contains(req.getLessonId()))
                    .build());
        }
        // the collection is orphan-removing: clear and refill it rather than replacing the list
        path.getItems().clear();
        path.getItems().addAll(items);
        recount(path);
    }

    /** The phase as stored: trimmed, a blank one is no phase. */
    private static String phaseOf(LearningPathItemCommand item) {
        String phase = item.getPhase() == null ? null : item.getPhase().trim();
        return phase == null || phase.isEmpty() ? null : phase;
    }

    private static void recount(UserLearningPath path) {
        int total = path.getItems().size();
        int completed = (int) path.getItems().stream().filter(item -> Boolean.TRUE.equals(item.getIsCompleted())).count();
        path.setTotalItems(total);
        path.setCompletedItems(completed);
        path.setCurrentProgress(total == 0 ? 0.0 : (double) completed / total * 100);
    }

    private void applyStatus(UserLearningPath path, boolean hasUnpaidCourse) {
        LearningPathStatus status = hasUnpaidCourse ? LearningPathStatus.PENDING_PAYMENT
                : isPathCompleted(path) ? LearningPathStatus.COMPLETED
                : LearningPathStatus.ACTIVE;
        if (status == LearningPathStatus.COMPLETED && path.getStatus() != LearningPathStatus.COMPLETED) {
            path.setCompletedAt(LocalDateTime.now(clock));
        }
        path.setStatus(status);
    }

    private static ConflictException conflictOf(DataIntegrityViolationException e) {
        String cause = String.valueOf(e.getMostSpecificCause().getMessage());
        if (cause.contains(ONE_ACTIVE_KEY)) {
            return new ConflictException(ErrorCode.LEARNING_PATH_ALREADY_ACTIVE, ALREADY_ACTIVE_MESSAGE);
        }
        // uk_learning_path_user_idempotency: the same key is being used by a create that has not finished
        return new ConflictException(ErrorCode.RESOURCE_ALREADY_EXISTS, "A create with this Idempotency-Key is already in progress");
    }

    private boolean isPathCompleted(UserLearningPath path) {
        return path.getTotalItems() != null
                && path.getCompletedItems() != null
                && path.getTotalItems() > 0
                && path.getTotalItems().equals(path.getCompletedItems());
    }
}
