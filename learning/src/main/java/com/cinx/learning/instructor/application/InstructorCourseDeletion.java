package com.cinx.learning.instructor.application;

import com.cinx.learning.instructor.domain.InstructorActivityRepository;
import com.cinx.learning.shared.application.CourseDeletionHook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
class InstructorCourseDeletion implements CourseDeletionHook {
    private final InstructorActivityRepository activityRepository;

    @Override
    public void onCourseDeleted(String courseId, Collection<String> lessonStableIds) {
        activityRepository.deleteAll(activityRepository.findAllByCourseId(courseId));
    }
}
