package com.cinx.learning.featured.application;

import com.cinx.learning.featured.domain.FeaturedCourse;
import com.cinx.learning.featured.domain.FeaturedCourseRepository;
import com.cinx.learning.featured.domain.FeaturedLearningPath;
import com.cinx.learning.featured.domain.FeaturedLearningPathRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeaturedCourseDeletionTest {
    @Mock
    private FeaturedCourseRepository featuredCourseRepository;
    @Mock
    private FeaturedLearningPathRepository featuredLearningPathRepository;

    @InjectMocks
    private FeaturedCourseDeletion deletion;

    @Test
    void theFeaturedRowsOfTheCourseGoAndOnlyThatCourseLeavesTheFeaturedPaths() {
        FeaturedCourse featured = new FeaturedCourse();
        FeaturedLearningPath path = new FeaturedLearningPath();
        path.setCourseIds(new ArrayList<>(List.of("course-1", "course-2", "course-1")));
        when(featuredCourseRepository.findAllByCourseId("course-1")).thenReturn(List.of(featured));
        when(featuredLearningPathRepository.findAllContainingCourse("course-1")).thenReturn(List.of(path));

        deletion.onCourseDeleted("course-1", List.of("lesson-a"));

        verify(featuredCourseRepository).deleteAll(List.of(featured));
        assertThat(path.getCourseIds()).containsExactly("course-2");
        verify(featuredLearningPathRepository).save(path);
    }
}
