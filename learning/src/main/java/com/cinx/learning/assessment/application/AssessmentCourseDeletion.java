package com.cinx.learning.assessment.application;

import com.cinx.learning.assessment.domain.AssignmentCriterionRepository;
import com.cinx.learning.shared.application.CourseDeletionHook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
class AssessmentCourseDeletion implements CourseDeletionHook {
    private final AssignmentCriterionRepository criterionRepository;

    @Override
    public void onCourseDeleted(String courseId, Collection<String> lessonStableIds) {
        // an assignment's criteria are keyed by its lesson's stable id; the other lessons simply have none
        lessonStableIds.forEach(stableId ->
                criterionRepository.deleteAll(criterionRepository.findAllByAssignmentIdOrderByOrderIndexAsc(stableId)));
    }
}
