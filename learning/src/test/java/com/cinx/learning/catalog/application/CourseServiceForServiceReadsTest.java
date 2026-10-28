package com.cinx.learning.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.application.mapper.CourseMapper;
import com.cinx.learning.catalog.domain.CategoryRepository;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.shared.domain.CourseStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Another service reads a course whatever its price: the anonymous reader-access rule must not hide paid courses. */
@ExtendWith(MockitoExtension.class)
class CourseServiceForServiceReadsTest {

    @Mock CourseRepository courseRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock InstructorDirectory userService;
    @Mock ICourseAccessService courseAccessService;
    @Mock CourseMapper courseMapper;
    @InjectMocks CourseService service;

    private static Course paid(String id) {
        Course course = new Course();
        course.setId(id);
        course.setInstructorId("instructor-1");
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPrice(new BigDecimal("199000"));
        return course;
    }

    @Test
    void oneCourseIsReadThroughTheCatalogVisibilityRuleNotTheReaderAccessRule() {
        Course course = paid("c1");
        when(courseAccessService.ensureViewableCourse(null, "c1")).thenReturn(course);
        when(userService.getInstructorById("instructor-1")).thenReturn(new ApiResponse<>(true, "ok", null));

        service.getCourseForService("c1");

        verify(courseMapper).toResponse(course, null, null);
        verify(courseAccessService, never()).ensureReadableCourse(any(), any());
    }

    @Test
    void paidCoursesComeBackInTheOrderAskedWithoutAnyAccessFilter() {
        Course first = paid("c1");
        Course second = paid("c2");
        when(courseRepository.findEnrolledReadableByIds(List.of("c2", "c1", "missing"))).thenReturn(List.of(first, second));
        when(userService.getInstructorsByIds(anyList())).thenReturn(new ApiResponse<>(true, "ok", List.of()));
        List<String> mapped = new ArrayList<>();
        when(courseMapper.toResponse(any(Course.class), any(), any())).thenAnswer(call -> {
            mapped.add(call.<Course>getArgument(0).getId());
            return null;
        });

        service.getCoursesForService(List.of("c2", "c1", "c2", "missing"));

        assertThat(mapped).containsExactly("c2", "c1");
        verify(courseAccessService, never()).canReadCourse(any(), any(Course.class), any());
    }
}
