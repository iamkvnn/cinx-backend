package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.UpdateCourseCommand;
import com.cinx.learning.catalog.application.event.LessonChangedEvent;
import com.cinx.learning.catalog.domain.Category;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseDraft;

import java.util.List;
import java.util.Optional;

public interface ICourseDraftService {
    Optional<CourseDraft> findDraft(Course course);
    CourseDraft getOrCreateDraft(Course course);
    CourseDraft createDraftFromCourse(Course course);
    CourseDraft updateDraft(Course course, UpdateCourseCommand request, Category category, Long discountRate);
    void markAiAssisted(Course course);
    List<LessonChangedEvent> approveDraft(Course course);
}

