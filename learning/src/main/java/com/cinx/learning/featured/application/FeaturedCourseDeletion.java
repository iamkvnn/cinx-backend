package com.cinx.learning.featured.application;

import com.cinx.learning.featured.domain.FeaturedCourseRepository;
import com.cinx.learning.featured.domain.FeaturedLearningPathRepository;
import com.cinx.learning.shared.application.CourseDeletionHook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
class FeaturedCourseDeletion implements CourseDeletionHook {
    private final FeaturedCourseRepository featuredCourseRepository;
    private final FeaturedLearningPathRepository featuredLearningPathRepository;

    @Override
    public void onCourseDeleted(String courseId, Collection<String> lessonStableIds) {
        featuredCourseRepository.deleteAll(featuredCourseRepository.findAllByCourseId(courseId));
        featuredLearningPathRepository.findAllContainingCourse(courseId).forEach(path -> {
            path.getCourseIds().removeIf(courseId::equals);
            featuredLearningPathRepository.save(path);
        });
    }
}
