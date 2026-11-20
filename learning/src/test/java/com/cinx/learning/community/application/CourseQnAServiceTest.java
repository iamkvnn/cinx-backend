package com.cinx.learning.community.application;

import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.community.application.command.CreateAnswerCommand;
import com.cinx.learning.community.application.command.CreateQnAReportCommand;
import com.cinx.learning.community.application.command.CreateQuestionCommand;
import com.cinx.learning.community.application.mapper.CourseQnAMapper;
import com.cinx.learning.community.application.query.AnswerView;
import com.cinx.learning.community.domain.AnswerHelpfulMark;
import com.cinx.learning.community.domain.AnswerHelpfulMarkRepository;
import com.cinx.learning.community.domain.AnswerUpvoteRepository;
import com.cinx.learning.community.domain.CourseAnswer;
import com.cinx.learning.community.domain.CourseAnswerRepository;
import com.cinx.learning.community.domain.CourseQuestion;
import com.cinx.learning.community.domain.CourseQuestionRepository;
import com.cinx.learning.community.domain.QuestionUpvoteRepository;
import com.cinx.learning.community.domain.ReportRepository;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseQnAServiceTest {
    @Mock
    private CourseQuestionRepository questionRepository;
    @Mock
    private CourseAnswerRepository answerRepository;
    @Mock
    private QuestionUpvoteRepository questionUpvoteRepository;
    @Mock
    private AnswerUpvoteRepository answerUpvoteRepository;
    @Mock
    private AnswerHelpfulMarkRepository answerHelpfulMarkRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private CourseQnAMapper mapper;
    @Mock
    private UserSummaryDirectory userClient;
    @Mock
    private LearningAuthorizationService authorizationService;
    @Mock
    private CourseQnAEvents eventPublisher;

    @InjectMocks
    private CourseQnAService service;

    @Test
    void markAnswerHelpfulStoresOnlyOneMarkForTheCurrentUser() {
        CourseAnswer answer = answer("answer-1", null);
        when(answerRepository.findById("answer-1")).thenReturn(Optional.of(answer));
        participates("user-1");
        when(answerHelpfulMarkRepository.existsByAnswerIdAndUserId("answer-1", "user-1"))
                .thenReturn(false, true);

        service.markAnswerHelpful("user-1", "answer-1");
        service.markAnswerHelpful("user-1", "answer-1");

        ArgumentCaptor<AnswerHelpfulMark> captor = ArgumentCaptor.forClass(AnswerHelpfulMark.class);
        verify(answerHelpfulMarkRepository).save(captor.capture());
        assertThat(captor.getValue().getAnswerId()).isEqualTo("answer-1");
        assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
    }

    @Test
    void markAnswerHelpfulKeepsDifferentUsersIndependent() {
        CourseAnswer answer = answer("answer-1", null);
        when(answerRepository.findById("answer-1")).thenReturn(Optional.of(answer));
        participates("user-1");
        participates("user-2");
        when(answerHelpfulMarkRepository.existsByAnswerIdAndUserId(any(), any())).thenReturn(false);

        service.markAnswerHelpful("user-1", "answer-1");
        service.markAnswerHelpful("user-2", "answer-1");

        verify(answerHelpfulMarkRepository, org.mockito.Mockito.times(2)).save(any(AnswerHelpfulMark.class));
    }

    @Test
    void markAnswerHelpfulRejectsUnknownAnswer() {
        when(answerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAnswerHelpful("user-1", "missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Answer not found");
        verify(answerHelpfulMarkRepository, never()).save(any());
    }

    @Test
    void unmarkAnswerHelpfulIsIdempotentAndScopedToCurrentUser() {
        when(answerRepository.findById("answer-1")).thenReturn(Optional.of(answer("answer-1", null)));
        participates("user-1");

        service.unmarkAnswerHelpful("user-1", "answer-1");
        service.unmarkAnswerHelpful("user-1", "answer-1");

        verify(answerHelpfulMarkRepository, org.mockito.Mockito.times(2))
                .deleteByAnswerIdAndUserId("answer-1", "user-1");
    }

    @Test
    void answerListExposesHelpfulFlagOnlyForTheCurrentUserAndLoadsMarksInBatch() {
        CourseAnswer answer = answer("answer-1", null);
        AnswerView dto = new AnswerView();
        participates("user-1");
        when(answerRepository.findByQuestionIdAndParentAnswerIdIsNullOrderByCreatedAtAsc(eq("question-1"), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(answer), 1, 10, 1));
        when(answerHelpfulMarkRepository.findAnswerIdsByUserIdAndAnswerIdIn("user-1", List.of("answer-1")))
                .thenReturn(List.of("answer-1"));
        when(mapper.toDto(answer)).thenReturn(dto);
        when(answerUpvoteRepository.existsByAnswerIdAndUserId("answer-1", "user-1"))
                .thenReturn(false);
        when(answerRepository.countByParentAnswerId("answer-1")).thenReturn(0);

        PageResult<AnswerView> result = service.getAnswersForQuestion("question-1", "user-1", 1, 10, null);

        assertThat(result.content()).singleElement()
                .extracting(AnswerView::getMarkedHelpfulByCurrentUser)
                .isEqualTo(true);
        verify(answerHelpfulMarkRepository).findAnswerIdsByUserIdAndAnswerIdIn("user-1", List.of("answer-1"));
    }

    @Test
    void replyListExposesHelpfulFlagForTheCurrentUser() {
        CourseAnswer reply = answer("reply-1", "answer-1");
        AnswerView dto = new AnswerView();
        when(answerRepository.findById("answer-1")).thenReturn(Optional.of(answer("answer-1", null)));
        participates("user-1");
        when(answerRepository.findByParentAnswerIdOrderByCreatedAtAsc(eq("answer-1"), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(reply), 1, 10, 1));
        when(answerHelpfulMarkRepository.findAnswerIdsByUserIdAndAnswerIdIn("user-1", List.of("reply-1")))
                .thenReturn(List.of("reply-1"));
        when(mapper.toDto(reply)).thenReturn(dto);
        when(answerUpvoteRepository.existsByAnswerIdAndUserId("reply-1", "user-1"))
                .thenReturn(false);
        when(answerRepository.countByParentAnswerId("reply-1")).thenReturn(0);

        PageResult<AnswerView> result = service.getReplies("answer-1", "user-1", 1, 10, null);

        assertThat(result.content()).singleElement()
                .extracting(AnswerView::getMarkedHelpfulByCurrentUser)
                .isEqualTo(true);
    }

    @Test
    void deletingAnswerTreeRemovesHelpfulMarksForAnswerAndReplies() {
        CourseAnswer answer = answer("answer-1", null);
        CourseAnswer reply = answer("reply-1", "answer-1");
        when(answerRepository.findById("answer-1")).thenReturn(Optional.of(answer));
        when(answerRepository.findByParentAnswerId("answer-1")).thenReturn(List.of(reply));
        when(answerRepository.findByParentAnswerId("reply-1")).thenReturn(List.of());

        service.deleteAnswerByAdmin("answer-1");

        verify(answerHelpfulMarkRepository).deleteByAnswerIdIn(List.of("answer-1", "reply-1"));
        verify(answerUpvoteRepository).deleteByAnswerIdIn(List.of("answer-1", "reply-1"));
    }

    @Test
    void deletingQuestionRemovesHelpfulMarksForAllItsAnswers() {
        CourseQuestion question = new CourseQuestion();
        question.setId("question-1");
        CourseAnswer answer = answer("answer-1", null);
        when(questionRepository.findById("question-1")).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionId("question-1")).thenReturn(List.of(answer));

        service.deleteQuestionByAdmin("question-1");

        verify(answerHelpfulMarkRepository).deleteByAnswerIdIn(List.of("answer-1"));
    }

    @Test
    void createAnswerKeepsTheAiLabelOnlyWhenDeclared() {
        CourseQuestion question = new CourseQuestion();
        question.setId("question-1");
        question.setCourseId("course-1");
        question.setUserId("learner-1");
        when(questionRepository.findById("question-1")).thenReturn(Optional.of(question));
        when(authorizationService.canParticipateInCourse("instructor-1", "course-1")).thenReturn(true);
        when(authorizationService.isCourseInstructor("course-1", "instructor-1")).thenReturn(true);
        when(mapper.toModel(any(CreateAnswerCommand.class))).thenAnswer(invocation -> new CourseAnswer());
        when(answerRepository.save(any(CourseAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(CourseAnswer.class))).thenAnswer(invocation -> new AnswerView());
        CreateAnswerCommand declared = new CreateAnswerCommand();
        declared.setContent("Use a cache.");
        declared.setAiAssisted(true);
        CreateAnswerCommand undeclared = new CreateAnswerCommand();
        undeclared.setContent("Use a cache.");

        service.createAnswer("instructor-1", "question-1", declared);
        service.createAnswer("instructor-1", "question-1", undeclared);

        ArgumentCaptor<CourseAnswer> saved = ArgumentCaptor.forClass(CourseAnswer.class);
        verify(answerRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(CourseAnswer::getAiAssisted).containsExactly(true, false);
    }

    @Test
    void outsiderCannotReadTheCourseQnA() {
        stubQuestionAndAnswer();

        assertNotEnrolled(() -> service.getQuestionsByCourse("course-1", null, "outsider", 1, 10, null, null, false));
        assertNotEnrolled(() -> service.findSimilarQuestions("course-1", null, "range(5) thiếu số 5", "outsider"));
        assertNotEnrolled(() -> service.getQuestionById("question-1", "outsider"));
        assertNotEnrolled(() -> service.getAnswersForQuestion("question-1", "outsider", 1, 10, null));
        assertNotEnrolled(() -> service.getReplies("answer-1", "outsider", 1, 10, null));

        verify(questionRepository, never()).search(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any());
        verify(questionRepository, never()).findSimilar(any(), any(), any(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyInt());
        verifyNoInteractions(mapper);
    }

    @Test
    void outsiderCannotWriteToTheCourseQnA() {
        stubQuestionAndAnswer();
        CreateQuestionCommand question = new CreateQuestionCommand();
        question.setCourseId("course-1");
        CreateAnswerCommand answer = new CreateAnswerCommand();
        answer.setContent("Me too.");
        CreateQnAReportCommand report = new CreateQnAReportCommand();
        report.setReason("spam");

        assertNotEnrolled(() -> service.createQuestion("outsider", question));
        assertNotEnrolled(() -> service.createAnswer("outsider", "question-1", answer));
        assertNotEnrolled(() -> service.upvoteQuestion("outsider", "question-1"));
        assertNotEnrolled(() -> service.upvoteAnswer("outsider", "answer-1"));
        assertNotEnrolled(() -> service.reportQuestion("outsider", "question-1", report));
        assertNotEnrolled(() -> service.reportAnswer("outsider", "answer-1", report));
        assertNotEnrolled(() -> service.markAnswerHelpful("outsider", "answer-1"));
        assertNotEnrolled(() -> service.unmarkAnswerHelpful("outsider", "answer-1"));

        verify(questionRepository, never()).save(any());
        verify(answerRepository, never()).save(any());
        verifyNoInteractions(questionUpvoteRepository, answerUpvoteRepository, answerHelpfulMarkRepository, reportRepository,
                eventPublisher);
    }

    @Test
    void teamMemberBelowManagerAnswersWithoutTheInstructorBadge() {
        participates("editor-1");
        when(mapper.toModel(any(CreateAnswerCommand.class))).thenAnswer(invocation -> new CourseAnswer());
        when(answerRepository.save(any(CourseAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(CourseAnswer.class))).thenAnswer(invocation -> new AnswerView());
        CreateAnswerCommand command = new CreateAnswerCommand();
        command.setContent("Look at 01:20.");

        service.createAnswer("editor-1", "question-1", command);

        ArgumentCaptor<CourseAnswer> saved = ArgumentCaptor.forClass(CourseAnswer.class);
        verify(answerRepository).save(saved.capture());
        assertThat(saved.getValue().getIsInstructorAnswer()).isFalse();
    }

    private void stubQuestionAndAnswer() {
        CourseQuestion question = new CourseQuestion();
        question.setId("question-1");
        question.setCourseId("course-1");
        when(questionRepository.findById("question-1")).thenReturn(Optional.of(question));
        when(answerRepository.findById("answer-1")).thenReturn(Optional.of(answer("answer-1", null)));
    }

    private void assertNotEnrolled(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_ENROLLED_IN_COURSE);
    }

    /** The user may take part in course-1, the course of question-1. */
    private void participates(String userId) {
        CourseQuestion question = new CourseQuestion();
        question.setId("question-1");
        question.setCourseId("course-1");
        when(questionRepository.findById("question-1")).thenReturn(Optional.of(question));
        when(authorizationService.canParticipateInCourse(userId, "course-1")).thenReturn(true);
    }

    private CourseAnswer answer(String id, String parentAnswerId) {
        CourseAnswer answer = new CourseAnswer();
        answer.setId(id);
        answer.setQuestionId("question-1");
        answer.setParentAnswerId(parentAnswerId);
        answer.setUserId("author-1");
        answer.setContent("answer");
        return answer;
    }
}
