package com.cinx.learning.assessment.application;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.assessment.application.mapper.AssignmentSubmissionMapper;
import com.cinx.learning.assessment.domain.AssignmentSubmissionAttachmentRepository;
import com.cinx.learning.assessment.domain.AssignmentSubmissionRepository;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.progress.application.ILearningProgressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {
    @Mock
    private AssignmentSubmissionAttachmentRepository assignmentSubmissionAttachmentRepository;
    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Mock
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    @Mock
    private ILearningProgressService learningProgressService;
    @Mock
    private IDailyGoalService dailyGoalService;
    @Mock
    private AssessmentAuthorization assessmentAuthorization;

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    void getAssignmentSubmissionsHandsTheSortParameterToTheStore() {
        ArgumentCaptor<PageQuery> query = ArgumentCaptor.forClass(PageQuery.class);
        when(assignmentSubmissionRepository.findAllByAssignmentId(eq("assignment-1"), query.capture()))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));

        assignmentService.getAssignmentSubmissions("assignment-1", 1, 10, "{\"submissionTime\":\"ASC\"}");

        assertThat(query.getValue().page()).isEqualTo(1);
        assertThat(query.getValue().size()).isEqualTo(10);
        assertThat(query.getValue().sort()).isEqualTo("{\"submissionTime\":\"ASC\"}");
    }
}
