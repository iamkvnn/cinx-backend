package com.cinx.learning.assessment.application;

import com.cinx.learning.assessment.domain.AssignmentCriterion;
import com.cinx.learning.assessment.domain.AssignmentCriterionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentCourseDeletionTest {
    @Mock
    private AssignmentCriterionRepository criterionRepository;

    @InjectMocks
    private AssessmentCourseDeletion deletion;

    @Test
    void theCriteriaOfEveryLessonOfTheCourseAreRemoved() {
        AssignmentCriterion criterion = new AssignmentCriterion();
        when(criterionRepository.findAllByAssignmentIdOrderByOrderIndexAsc("assignment-1")).thenReturn(List.of(criterion));
        when(criterionRepository.findAllByAssignmentIdOrderByOrderIndexAsc("video-1")).thenReturn(List.of());

        deletion.onCourseDeleted("course-1", List.of("assignment-1", "video-1"));

        verify(criterionRepository).deleteAll(List.of(criterion));
        // a lesson that has no criteria simply removes nothing
        verify(criterionRepository).deleteAll(List.of());
    }
}
