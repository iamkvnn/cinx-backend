package com.cinx.learning.catalog.application;

import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.application.query.DraftChanges;
import com.cinx.learning.catalog.application.query.DraftChanges.FieldChange;
import com.cinx.learning.catalog.application.query.DraftChanges.LessonChange;
import com.cinx.learning.catalog.application.query.DraftChanges.LessonChangeKind;
import com.cinx.learning.catalog.application.query.DraftChanges.LessonChanges;
import com.cinx.learning.catalog.application.query.DraftChanges.LessonRef;
import com.cinx.learning.catalog.application.query.DraftChanges.SectionChange;
import com.cinx.learning.catalog.application.query.DraftChanges.SectionChanges;
import com.cinx.learning.catalog.application.query.DraftChanges.SectionRef;
import com.cinx.learning.catalog.domain.Category;
import com.cinx.learning.catalog.domain.CategoryRepository;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseDraft;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.Section;
import com.cinx.learning.catalog.domain.LessonRepository;
import com.cinx.learning.catalog.domain.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseChangeService implements ICourseChangeService {
    private final ICourseAccessService courseAccessService;
    private final ICourseDraftService courseDraftService;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public DraftChanges getDraftChanges(String currentUserId, String courseId) {
        return changesOf(courseAccessService.ensureManageableCourse(currentUserId, courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public DraftChanges getDraftChangesForReview(String courseId) {
        return changesOf(courseAccessService.reviewableCourse(courseId));
    }

    private DraftChanges changesOf(Course course) {
        String courseId = course.getId();
        boolean hasLiveVersion = course.getStatus() == CourseStatus.PUBLISHED;
        Optional<CourseDraft> draft = hasLiveVersion ? courseDraftService.findDraft(course) : Optional.empty();
        if (draft.isEmpty()) {
            return DraftChanges.none(hasLiveVersion);
        }
        List<Section> liveSections = sectionRepository.findPublishedByCourse(courseId);
        List<Section> draftSections = sectionRepository.findDraftByDraft(draft.get().getId());
        Map<String, Section> sectionsById = new HashMap<>();
        liveSections.forEach(s -> sectionsById.put(s.getId(), s));
        draftSections.forEach(s -> sectionsById.put(s.getId(), s));
        return new DraftChanges(true,
                courseFields(course, draft.get()),
                sectionChanges(liveSections, draftSections),
                lessonChanges(lessonRepository.findPublishedByCourse(courseId), lessonRepository.findDraftByDraft(draft.get().getId()), sectionsById));
    }

    private List<FieldChange> courseFields(Course live, CourseDraft draft) {
        List<FieldChange> changes = new ArrayList<>();
        addIfDifferent(changes, "title", live.getTitle(), draft.getTitle());
        addIfDifferent(changes, "description", live.getDescription(), draft.getDescription());
        addIfDifferent(changes, "category", categoryName(live.getCategoryId()), categoryName(draft.getCategoryId()));
        addIfDifferent(changes, "price", live.getPrice(), draft.getPrice());
        addIfDifferent(changes, "discountedPrice", live.getDiscountedPrice(), draft.getDiscountedPrice());
        addIfDifferent(changes, "duration", live.getDuration(), draft.getDuration());
        addIfDifferent(changes, "hasCertificate", live.getHasCertificate(), draft.getHasCertificate());
        addIfDifferent(changes, "certificateTitle", live.getCertificateTitle(), draft.getCertificateTitle());
        addIfDifferent(changes, "outcomes", List.copyOf(live.getOutcomes()), List.copyOf(draft.getOutcomes()));
        return changes;
    }

    private static void addIfDifferent(List<FieldChange> changes, String field, Object before, Object after) {
        if (!same(before, after)) {
            changes.add(new FieldChange(field, before, after));
        }
    }

    private static boolean same(Object before, Object after) {
        if (before instanceof BigDecimal a && after instanceof BigDecimal b) {
            return a.compareTo(b) == 0;
        }
        return Objects.equals(before, after);
    }

    private String categoryName(String categoryId) {
        return categoryId == null ? null : categoryRepository.findById(categoryId).map(Category::getName).orElse(null);
    }

    private SectionChanges sectionChanges(List<Section> live, List<Section> draft) {
        Map<String, Section> liveById = byStableId(live, Section::getStableId);
        Map<String, Section> draftById = byStableId(draft, Section::getStableId);

        List<SectionRef> added = draft.stream().filter(s -> !liveById.containsKey(s.getStableId())).map(CourseChangeService::ref).toList();
        List<SectionRef> removed = live.stream().filter(s -> !draftById.containsKey(s.getStableId())).map(CourseChangeService::ref).toList();
        List<SectionChange> renamed = draft.stream()
                .filter(s -> liveById.containsKey(s.getStableId()))
                .filter(s -> !Objects.equals(liveById.get(s.getStableId()).getTitle(), s.getTitle()))
                .map(s -> new SectionChange(s.getStableId(), liveById.get(s.getStableId()).getTitle(), s.getTitle()))
                .toList();

        List<String> liveOrder = live.stream().map(Section::getStableId).filter(draftById::containsKey).toList();
        List<String> draftOrder = draft.stream().map(Section::getStableId).filter(liveById::containsKey).toList();
        Set<String> keptInPlace = keptInPlace(liveOrder, draftOrder);
        List<SectionRef> moved = draft.stream()
                .filter(s -> liveById.containsKey(s.getStableId()) && !keptInPlace.contains(s.getStableId()))
                .map(CourseChangeService::ref)
                .toList();
        return new SectionChanges(added, removed, renamed, moved);
    }

    private LessonChanges lessonChanges(List<Lesson> live, List<Lesson> draft, Map<String, Section> sections) {
        Map<String, Lesson> liveById = byStableId(live, Lesson::getStableId);
        Map<String, Lesson> draftById = byStableId(draft, Lesson::getStableId);

        List<LessonRef> added = draft.stream().filter(l -> !liveById.containsKey(l.getStableId())).map(l -> ref(l, sections)).toList();
        List<LessonRef> removed = live.stream().filter(l -> !draftById.containsKey(l.getStableId())).map(l -> ref(l, sections)).toList();

        // Lessons that stayed in the same section: the ones that left their place there were reordered.
        Map<String, List<String>> liveBySection = new LinkedHashMap<>();
        Map<String, List<String>> draftBySection = new LinkedHashMap<>();
        for (Lesson lesson : live) {
            Lesson other = draftById.get(lesson.getStableId());
            if (other != null && sameSection(lesson, other, sections)) {
                liveBySection.computeIfAbsent(sectionStableId(lesson, sections), k -> new ArrayList<>()).add(lesson.getStableId());
            }
        }
        for (Lesson lesson : draft) {
            Lesson other = liveById.get(lesson.getStableId());
            if (other != null && sameSection(other, lesson, sections)) {
                draftBySection.computeIfAbsent(sectionStableId(lesson, sections), k -> new ArrayList<>()).add(lesson.getStableId());
            }
        }
        Set<String> keptInPlace = new HashSet<>();
        liveBySection.forEach((sectionId, order) -> keptInPlace.addAll(keptInPlace(order, draftBySection.get(sectionId))));

        List<LessonChange> changed = new ArrayList<>();
        for (Lesson lesson : draft) {
            Lesson before = liveById.get(lesson.getStableId());
            if (before == null) {
                continue;
            }
            List<LessonChangeKind> kinds = new ArrayList<>();
            if (!Objects.equals(before.getTitle(), lesson.getTitle())) {
                kinds.add(LessonChangeKind.TITLE);
            }
            if (!Objects.equals(before.getDuration(), lesson.getDuration())) {
                kinds.add(LessonChangeKind.DURATION);
            }
            if (!Objects.equals(before.getIsPreview(), lesson.getIsPreview())) {
                kinds.add(LessonChangeKind.PREVIEW);
            }
            if (!prerequisites(before).equals(prerequisites(lesson))) {
                kinds.add(LessonChangeKind.PREREQUISITES);
            }
            if (!sameSection(before, lesson, sections)) {
                kinds.add(LessonChangeKind.MOVED);
            } else if (!keptInPlace.contains(lesson.getStableId())) {
                kinds.add(LessonChangeKind.REORDERED);
            }
            if (!kinds.isEmpty()) {
                changed.add(new LessonChange(lesson.getStableId(), lesson.getTitle(), sectionTitle(lesson, sections), kinds));
            }
        }
        return new LessonChanges(added, removed, changed);
    }

    /** The longest run of {@code oldOrder} still in order in {@code newOrder}, so moving one of ten flags one. */
    static Set<String> keptInPlace(List<String> oldOrder, List<String> newOrder) {
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < newOrder.size(); i++) {
            rank.put(newOrder.get(i), i);
        }
        int n = oldOrder.size();
        int[] length = new int[n];
        int[] previous = new int[n];
        int best = -1;
        for (int i = 0; i < n; i++) {
            length[i] = 1;
            previous[i] = -1;
            for (int j = 0; j < i; j++) {
                if (rank.get(oldOrder.get(j)) < rank.get(oldOrder.get(i)) && length[j] + 1 > length[i]) {
                    length[i] = length[j] + 1;
                    previous[i] = j;
                }
            }
            if (best < 0 || length[i] > length[best]) {
                best = i;
            }
        }
        Set<String> kept = new HashSet<>();
        for (int i = best; i >= 0; i = previous[i]) {
            kept.add(oldOrder.get(i));
        }
        return kept;
    }

    private static boolean sameSection(Lesson live, Lesson draft, Map<String, Section> sections) {
        return Objects.equals(sectionStableId(live, sections), sectionStableId(draft, sections));
    }

    private static String sectionStableId(Lesson lesson, Map<String, Section> sections) {
        Section section = sections.get(lesson.getSectionId());
        return section == null ? null : section.getStableId();
    }

    private static String sectionTitle(Lesson lesson, Map<String, Section> sections) {
        Section section = sections.get(lesson.getSectionId());
        return section == null ? null : section.getTitle();
    }

    private static Set<String> prerequisites(Lesson lesson) {
        return lesson.getPrerequisiteIds() == null ? Set.of() : new HashSet<>(lesson.getPrerequisiteIds());
    }

    private static <T> Map<String, T> byStableId(List<T> items, Function<T, String> stableId) {
        return items.stream().collect(Collectors.toMap(stableId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static SectionRef ref(Section section) {
        return new SectionRef(section.getStableId(), section.getTitle());
    }

    private static LessonRef ref(Lesson lesson, Map<String, Section> sections) {
        return new LessonRef(lesson.getStableId(), lesson.getTitle(), sectionTitle(lesson, sections), lesson.getLessonType());
    }
}
