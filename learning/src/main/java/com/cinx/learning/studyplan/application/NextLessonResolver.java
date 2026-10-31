package com.cinx.learning.studyplan.application;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.query.LessonInfo;
import com.cinx.learning.catalog.application.query.VideoLessonInfo;
import com.cinx.learning.progress.application.ProgressLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The lesson a learner resumes in one course ("Học tiếp"): the last lesson touched, or the next unfinished one after it
 * when it is already done. Without a last lesson it is the first unfinished one. Shared by the resume card and
 * "Học tập của tôi" so both point at the same lesson.
 */
@Component
@RequiredArgsConstructor
public class NextLessonResolver {
    private final CatalogLookup catalogLookup;
    private final ProgressLookup progressLookup;

    /** The resolved lesson, its 1-based chapter and in-chapter positions, where the video stopped and the minutes left. */
    public record Next(LessonInfo lesson, int sectionPosition, int lessonPosition, Integer positionSeconds, int remainingMinutes) {
    }

    /**
     * @param lessons   readable lessons of one course, in course order; not empty
     * @param completed stable ids of the lessons the learner has completed
     * @param lastItemId the last lesson touched, or null
     */
    public Next resolve(String userId, List<LessonInfo> lessons, Set<String> completed, String lastItemId) {
        int index = 0;
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getStableId().equals(lastItemId)) {
                index = i;
                break;
            }
        }
        LessonInfo target = lessons.get(index);
        if (completed.contains(target.getStableId())) {
            target = lessons.stream().skip(index + 1L)
                    .filter(lesson -> !completed.contains(lesson.getStableId()))
                    .findFirst()
                    .orElse(target);
        }
        Integer position = null;
        int remaining = StudyPlanScheduler.estimateMinutes(target.getLessonType(), null);
        if (target.getLessonType() == LessonType.VIDEO) {
            Integer seconds = catalogLookup.videoLesson(target.getStableId()).map(VideoLessonInfo::getDuration).orElse(null);
            position = progressLookup.videoPosition(userId, target.getStableId()).orElse(null);
            int total = StudyPlanScheduler.estimateMinutes(LessonType.VIDEO, seconds);
            remaining = seconds != null && position != null
                    ? Math.max(1, (Math.max(0, seconds - position) + 59) / 60)
                    : total;
        }
        return new Next(target, sectionPosition(lessons, target), lessonPosition(lessons, target), position, remaining);
    }

    private static int sectionPosition(List<LessonInfo> lessons, LessonInfo target) {
        String section = sectionId(target);
        return (int) lessons.stream()
                .map(NextLessonResolver::sectionId)
                .distinct()
                .takeWhile(id -> !Objects.equals(id, section))
                .count() + 1;
    }

    private static int lessonPosition(List<LessonInfo> lessons, LessonInfo target) {
        String section = sectionId(target);
        int position = 0;
        for (LessonInfo lesson : lessons) {
            if (Objects.equals(sectionId(lesson), section)) {
                position++;
                if (lesson == target) {
                    return position;
                }
            }
        }
        return position;
    }

    private static String sectionId(LessonInfo lesson) {
        return lesson.getSectionId();
    }
}
