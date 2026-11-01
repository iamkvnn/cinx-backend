package com.cinx.learning.community.application;

import com.cinx.learning.community.application.command.BatchAnswerCommand;
import com.cinx.learning.community.application.command.CreateAnswerCommand;
import com.cinx.learning.community.application.query.AnswerView;
import com.cinx.learning.community.domain.AnswerHelpfulMarkRepository;
import com.cinx.learning.community.domain.CourseAnswerRepository;
import com.cinx.learning.community.domain.CourseQuestion;
import com.cinx.learning.community.domain.CourseQuestionRepository;
import com.cinx.learning.instructor.application.IInstructorActivityService;
import com.cinx.learning.instructor.application.InstructorCourseLookup;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorQnAServiceTest {
    @Mock
    private CourseQuestionRepository questionRepository;
    @Mock
    private CourseAnswerRepository answerRepository;
    @Mock
    private AnswerHelpfulMarkRepository helpfulMarkRepository;
    @Mock
    private ICourseQnAService qnaService;
    @Mock
    private LearningAuthorizationService authorizationService;
    @Mock
    private IInstructorActivityService activityService;
    @Mock
    private InstructorCourseLookup courseLookup;
    @Mock
    private UserSummaryDirectory userClient;

    @InjectMocks
    private InstructorQnAService service;

    @Test
    void answeringTogetherCarriesTheAiLabelToEveryCopy() {
        when(questionRepository.findAllById(List.of("q-1", "q-2"))).thenReturn(List.of(question("q-1"), question("q-2")));
        when(authorizationService.isCourseInstructor("course-1", "instructor-1")).thenReturn(true);
        when(qnaService.createAnswer(eq("instructor-1"), any(), any())).thenAnswer(invocation -> {
            AnswerView view = new AnswerView();
            view.setId("a-" + invocation.getArgument(1));
            return view;
        });

        service.answerTogether("instructor-1", new BatchAnswerCommand(List.of("q-1", "q-2"), "Use a cache.", true));

        ArgumentCaptor<CreateAnswerCommand> sent = ArgumentCaptor.forClass(CreateAnswerCommand.class);
        verify(qnaService, times(2)).createAnswer(eq("instructor-1"), any(), sent.capture());
        assertThat(sent.getAllValues()).extracting(CreateAnswerCommand::getAiAssisted).containsExactly(true, true);
    }

    private static CourseQuestion question(String id) {
        CourseQuestion question = new CourseQuestion();
        question.setId(id);
        question.setCourseId("course-1");
        question.setTitle("Why cache?");
        return question;
    }
}
