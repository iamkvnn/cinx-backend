package com.cinx.learning.community.application;

import com.cinx.common.paging.PageQuery;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.community.application.UserSummaryDirectory;
import com.cinx.learning.community.application.command.BatchAnswerCommand;
import com.cinx.learning.community.application.command.CreateAnswerCommand;
import com.cinx.learning.community.application.query.AnswerView;
import com.cinx.learning.community.application.query.InstructorQnAResponses;
import com.cinx.learning.community.application.query.UserSummaryView;
import com.cinx.learning.community.domain.CourseAnswer;
import com.cinx.learning.community.domain.CourseQuestion;
import com.cinx.learning.community.domain.AnswerHelpfulMarkRepository;
import com.cinx.learning.community.domain.CourseAnswerRepository;
import com.cinx.learning.community.domain.CourseQuestionRepository;
import com.cinx.learning.shared.domain.ActivityAction;
import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.application.IInstructorActivityService;
import com.cinx.learning.instructor.application.InstructorCourseLookup;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstructorQnAService implements IInstructorQnAService {
    static final int PAST_ANSWERS = 20;

    private final CourseQuestionRepository questionRepository;
    private final CourseAnswerRepository answerRepository;
    private final AnswerHelpfulMarkRepository helpfulMarkRepository;
    private final ICourseQnAService qnaService;
    private final LearningAuthorizationService authorizationService;
    private final IInstructorActivityService activityService;
    private final InstructorCourseLookup courseLookup;
    private final UserSummaryDirectory userClient;

    @Override
    @Transactional(readOnly = true)
    public List<InstructorQnAResponses.Question> unansweredInLesson(String currentUserId, String lessonId) {
        authorizationService.requireLessonInstructor(currentUserId, lessonId);
        List<CourseQuestion> questions = questionRepository.findUnansweredByLessonId(lessonId);
        InstructorCourseLookup.LessonRef lesson = courseLookup.lessonsByStableId(List.of(lessonId)).get(lessonId);
        Map<String, UserSummaryView> users = usersById(questions.stream().map(CourseQuestion::getUserId).toList());
        return questions.stream()
                .map(q -> {
                    UserSummaryView user = users.get(q.getUserId());
                    return new InstructorQnAResponses.Question(
                            q.getId(),
                            q.getCourseId(),
                            q.getLessonId(),
                            lesson == null ? null : lesson.title(),
                            q.getUserId(),
                            user == null ? null : user.name(),
                            user == null ? null : user.avatarUrl(),
                            q.getTitle(),
                            q.getContent(),
                            q.getUpvoteCount() == null ? 0 : q.getUpvoteCount(),
                            toInstant(q.getCreatedAt()),
                            false);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorQnAResponses.PastAnswer> myAnswersInLesson(String currentUserId, String lessonId) {
        authorizationService.requireLessonInstructor(currentUserId, lessonId);
        List<CourseAnswer> answers = answerRepository.findInstructorAnswersInLesson(lessonId, currentUserId, PageQuery.first(PAST_ANSWERS));
        Map<String, String> titles = questionRepository.findAllById(answers.stream().map(CourseAnswer::getQuestionId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(CourseQuestion::getId, CourseQuestion::getTitle, (a, b) -> a));
        return answers.stream()
                .map(a -> new InstructorQnAResponses.PastAnswer(
                        a.getId(),
                        a.getQuestionId(),
                        titles.get(a.getQuestionId()),
                        a.getContent(),
                        toInstant(a.getCreatedAt()),
                        helpfulMarkRepository.countByAnswerId(a.getId())))
                .toList();
    }

    /**
     * Every question must belong to a course the caller instructs, else nothing is posted. Each question then gets its
     * own instructor answer through the single-answer path, so each asker is notified by the existing event.
     */
    @Override
    @Transactional
    public InstructorQnAResponses.BatchResult answerTogether(String currentUserId, BatchAnswerCommand request) {
        List<String> ids = request.questionIds().stream().distinct().toList();
        Map<String, CourseQuestion> questions = questionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(CourseQuestion::getId, Function.identity()));
        for (String id : ids) {
            CourseQuestion question = questions.get(id);
            if (question == null || !authorizationService.isCourseInstructor(question.getCourseId(), currentUserId)) {
                throw new NotFoundException("Question not found");
            }
        }

        List<InstructorQnAResponses.Result> results = new ArrayList<>();
        for (String id : ids) {
            CreateAnswerCommand answer = new CreateAnswerCommand();
            answer.setQuestionId(id);
            answer.setContent(request.content());
            answer.setAiAssisted(request.aiAssisted());
            AnswerView created = qnaService.createAnswer(currentUserId, id, answer);
            results.add(new InstructorQnAResponses.Result(id, created.getId(), true, null));
        }

        CourseQuestion first = questions.get(ids.get(0));
        activityService.record(currentUserId, first.getCourseId(), InboxKind.QUESTIONS,
                ids.size() > 1 ? ActivityAction.ANSWERED_TOGETHER : ActivityAction.ANSWERED,
                first.getLessonId(), ids.size(), first.getTitle());
        return new InstructorQnAResponses.BatchResult(results);
    }

    /** Best-effort, like the Q&A reads: a failed lookup leaves the learner's name empty rather than failing. */
    private Map<String, UserSummaryView> usersById(Collection<String> ids) {
        LinkedHashSet<String> userIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return Map.of();
        }
        try {
            ApiResponse<List<UserSummaryView>> response = userClient.getUsersByIds(List.copyOf(userIds));
            if (response == null || !response.success() || response.data() == null) {
                return Map.of();
            }
            return response.data().stream()
                    .filter(user -> user.userId() != null)
                    .collect(Collectors.toMap(UserSummaryView::userId, Function.identity(), (a, b) -> a));
        } catch (Exception ex) {
            log.warn("Failed to fetch question author summaries", ex);
            return Map.of();
        }
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
