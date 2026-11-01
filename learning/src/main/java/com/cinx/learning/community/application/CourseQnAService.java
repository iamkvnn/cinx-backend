package com.cinx.learning.community.application;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.community.application.UserSummaryDirectory;
import com.cinx.learning.community.application.command.CreateAnswerCommand;
import com.cinx.learning.community.application.command.CreateQuestionCommand;
import com.cinx.learning.community.application.command.UpdateAnswerCommand;
import com.cinx.learning.community.application.command.UpdateQuestionCommand;
import com.cinx.learning.community.application.query.AnswerView;
import com.cinx.learning.community.application.query.QuestionView;
import com.cinx.learning.community.application.query.UserSummaryView;
import com.cinx.learning.community.application.event.CourseAnswerCreatedEvent;
import com.cinx.learning.community.application.event.CourseQuestionCreatedEvent;
import com.cinx.learning.community.application.mapper.CourseQnAMapper;
import com.cinx.learning.community.application.CourseQnAEvents;
import com.cinx.learning.community.domain.AnswerHelpfulMark;
import com.cinx.learning.community.domain.AnswerUpvote;
import com.cinx.learning.community.domain.CourseAnswer;
import com.cinx.learning.community.domain.CourseQuestion;
import com.cinx.learning.community.domain.QuestionUpvote;
import com.cinx.learning.community.domain.AnswerUpvoteRepository;
import com.cinx.learning.community.domain.AnswerHelpfulMarkRepository;
import com.cinx.learning.community.domain.CourseAnswerRepository;
import com.cinx.learning.community.domain.CourseQuestionRepository;
import com.cinx.learning.community.domain.QuestionUpvoteRepository;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import com.cinx.learning.community.domain.Report;
import com.cinx.learning.community.domain.ReportType;
import com.cinx.learning.community.domain.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseQnAService implements ICourseQnAService {
    private static final int SIMILAR_MIN_CHARS = 8;
    private static final int SIMILAR_MAX_CHARS = 300;
    private static final int SIMILAR_LIMIT = 5;
    // InnoDB relevance, 2026-10-13 probe of 12 questions: real matches 0.6-5.5, a lone common word such as "lỗi" <= 0.29
    private static final double SIMILAR_MIN_SCORE = 0.5;

    private final CourseQuestionRepository questionRepository;
    private final CourseAnswerRepository answerRepository;
    private final QuestionUpvoteRepository questionUpvoteRepository;
    private final AnswerUpvoteRepository answerUpvoteRepository;
    private final AnswerHelpfulMarkRepository answerHelpfulMarkRepository;
    private final ReportRepository reportRepository;
    private final CourseQnAMapper mapper;
    private final UserSummaryDirectory userClient;
    private final LearningAuthorizationService authorizationService;
    private final CourseQnAEvents eventPublisher;

    private void requireParticipant(String userId, String courseId) {
        if (!authorizationService.canParticipateInCourse(userId, courseId)) {
            throw new ForbiddenException(ErrorCode.NOT_ENROLLED_IN_COURSE, "Not enrolled in this course");
        }
    }

    /** The answer, after checking that the user may take part in the course of its question. */
    private CourseAnswer requireParticipantOfAnswer(String userId, String answerId) {
        CourseAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found"));
        CourseQuestion question = questionRepository.findById(answer.getQuestionId())
                .orElseThrow(() -> new NotFoundException("Question not found"));
        requireParticipant(userId, question.getCourseId());
        return answer;
    }

    private boolean isCourseInstructor(String userId, String courseId) {
        return authorizationService.isCourseInstructor(courseId, userId);
    }

    @Override
    @Transactional
    public QuestionView createQuestion(String userId, CreateQuestionCommand request) {
        requireParticipant(userId, request.getCourseId());

        CourseQuestion question = mapper.toModel(request);
        question.setUserId(userId);
        question.setUpvoteCount(defaultCount(question.getUpvoteCount()));
        question = questionRepository.save(question);

        CourseQuestionCreatedEvent event = CourseQuestionCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .questionId(question.getId())
                .courseId(question.getCourseId())
                .lessonId(question.getLessonId())
                .askedByUserId(userId)
                .questionTitle(question.getTitle())
                .occurredAt(Instant.now())
                .build();
        eventPublisher.publishQuestionCreatedEvent(event);

        return withAuthor(mapQuestionToDtoWithAnswersCount(question, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionView> getQuestionsByCourse(String courseId, String lessonId, String currentUserId, int page, int size, String query, String sort, boolean unanswered) {
        requireParticipant(currentUserId, courseId);
        PageQuery pageable = new PageQuery(page, size, sort);
        PageResult<CourseQuestion> courseQuestionPage = questionRepository.search(courseId, normalizeQuery(lessonId), normalizeQuery(query), unanswered, pageable);
        PageResult<QuestionView> questions = courseQuestionPage.map(q -> mapQuestionToDtoWithAnswersCount(q, currentUserId));
        attachQuestionAuthors(questions.content());
        return questions;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionView getQuestionById(String questionId, String currentUserId) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));
        requireParticipant(currentUserId, question.getCourseId());

        return withAuthor(mapQuestionToDtoWithAnswersCount(question, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionView> findSimilarQuestions(String courseId, String lessonId, String text, String currentUserId) {
        requireParticipant(currentUserId, courseId);
        String trimmed = text == null ? "" : text.strip();
        if (trimmed.length() < SIMILAR_MIN_CHARS) {
            return List.of();
        }
        String terms = QuestionSearchTerms.of(trimmed.substring(0, Math.min(trimmed.length(), SIMILAR_MAX_CHARS)));
        if (terms.isEmpty()) {
            return List.of();
        }
        List<QuestionView> questions = questionRepository.findSimilar(courseId, normalizeQuery(lessonId), terms, SIMILAR_MIN_SCORE, SIMILAR_LIMIT)
                .stream().map(q -> mapQuestionToDtoWithAnswersCount(q, currentUserId)).toList();
        attachQuestionAuthors(questions);
        return questions;
    }

    private QuestionView mapQuestionToDtoWithAnswersCount(CourseQuestion question, String currentUserId) {
        QuestionView dto = mapper.toDto(question);
        if (currentUserId != null) {
            dto.setHasUpvoted(questionUpvoteRepository.existsByQuestionIdAndUserId(question.getId(), currentUserId));
        }
        dto.setAnswersCount(answerRepository.countByQuestionId(question.getId()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AnswerView> getAnswersForQuestion(String questionId, String currentUserId, int page, int size, String sort) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));
        requireParticipant(currentUserId, question.getCourseId());
        PageQuery pageable = new PageQuery(page, size, sort);
        PageResult<CourseAnswer> answers = answerRepository.findByQuestionIdAndParentAnswerIdIsNullOrderByCreatedAtAsc(questionId, pageable);
        Set<String> markedAnswerIds = markedAnswerIds(currentUserId, answers.content());
        PageResult<AnswerView> dtos = answers.map(a -> mapAnswerToDtoWithRepliesCount(a, currentUserId, markedAnswerIds));
        attachAnswerAuthors(dtos.content());
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AnswerView> getReplies(String parentAnswerId, String currentUserId, int page, int size, String sort) {
        requireParticipantOfAnswer(currentUserId, parentAnswerId);
        PageQuery pageable = new PageQuery(page, size, sort);
        PageResult<CourseAnswer> replies = answerRepository.findByParentAnswerIdOrderByCreatedAtAsc(parentAnswerId, pageable);
        Set<String> markedAnswerIds = markedAnswerIds(currentUserId, replies.content());
        PageResult<AnswerView> dtos = replies.map(r -> mapAnswerToDtoWithRepliesCount(r, currentUserId, markedAnswerIds));
        attachAnswerAuthors(dtos.content());
        return dtos;
    }

    private AnswerView mapAnswerToDtoWithRepliesCount(CourseAnswer answer, String currentUserId) {
        return mapAnswerToDtoWithRepliesCount(answer, currentUserId, markedAnswerIds(currentUserId, List.of(answer)));
    }

    private AnswerView mapAnswerToDtoWithRepliesCount(CourseAnswer answer, String currentUserId, Set<String> markedAnswerIds) {
        AnswerView dto = mapper.toDto(answer);
        dto.setMarkedHelpfulByCurrentUser(markedAnswerIds.contains(answer.getId()));
        if (currentUserId != null) {
            dto.setHasUpvoted(answerUpvoteRepository.existsByAnswerIdAndUserId(answer.getId(), currentUserId));
        }
        dto.setRepliesCount(answerRepository.countByParentAnswerId(answer.getId()));
        return dto;
    }

    private QuestionView withAuthor(QuestionView dto) {
        attachQuestionAuthors(List.of(dto));
        return dto;
    }

    private AnswerView withAuthor(AnswerView dto) {
        attachAnswerAuthors(List.of(dto));
        return dto;
    }

    private void attachQuestionAuthors(List<QuestionView> questions) {
        Map<String, UserSummaryView> usersById = fetchUsersById(questions.stream().map(QuestionView::getUserId).toList());
        questions.forEach(q -> q.setUser(usersById.get(q.getUserId())));
    }

    private void attachAnswerAuthors(List<AnswerView> answers) {
        Map<String, UserSummaryView> usersById = fetchUsersById(answers.stream().map(AnswerView::getUserId).toList());
        answers.forEach(a -> a.setUser(usersById.get(a.getUserId())));
    }

    /** Best-effort enrichment, as ReviewService does: a failed lookup leaves `user` null rather than failing the read. */
    private Map<String, UserSummaryView> fetchUsersById(Collection<String> ids) {
        LinkedHashSet<String> userIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        ApiResponse<List<UserSummaryView>> response;
        try {
            response = userClient.getUsersByIds(List.copyOf(userIds));
        } catch (Exception ex) {
            log.warn("Failed to fetch Q&A author summaries", ex);
            return Collections.emptyMap();
        }
        if (response == null || !response.success() || response.data() == null) {
            return Collections.emptyMap();
        }
        return response.data().stream()
                .filter(user -> user.userId() != null)
                .collect(Collectors.toMap(UserSummaryView::userId, Function.identity(), (first, second) -> first));
    }

    private Set<String> markedAnswerIds(String currentUserId, List<CourseAnswer> answers) {
        if (currentUserId == null || answers.isEmpty()) {
            return Set.of();
        }
        List<String> answerIds = answers.stream()
                .map(CourseAnswer::getId)
                .toList();
        return new HashSet<>(answerHelpfulMarkRepository.findAnswerIdsByUserIdAndAnswerIdIn(currentUserId, answerIds));
    }

    @Override
    @Transactional
    public QuestionView updateQuestion(String userId, String questionId, UpdateQuestionCommand request) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));
        if (!question.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "Not the owner");
        }
        mapper.partialUpdate(question, request);
        question = questionRepository.save(question);
        return withAuthor(mapQuestionToDtoWithAnswersCount(question, userId));
    }

    @Override
    @Transactional
    public void deleteQuestion(String userId, String questionId) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));
        if (!question.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "Not the owner");
        }
        deleteQuestionContent(question);
    }

    @Override
    @Transactional
    public void deleteQuestionByAdmin(String questionId) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));
        deleteQuestionContent(question);
    }

    private void deleteQuestionContent(CourseQuestion question) {
        List<String> answerIds = answerRepository.findByQuestionId(question.getId()).stream()
                .map(CourseAnswer::getId)
                .toList();
        deleteAnswersByIds(answerIds);
        questionUpvoteRepository.deleteByQuestionId(question.getId());
        reportRepository.deleteByRefIdAndType(question.getId(), ReportType.QUESTION);
        questionRepository.delete(question);
    }

    @Override
    @Transactional
    public void upvoteQuestion(String userId, String questionId) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        requireParticipant(userId, question.getCourseId());

        QuestionUpvote existingUpvote = questionUpvoteRepository.findByQuestionIdAndUserId(questionId, userId)
                .orElse(null);
        if (existingUpvote != null) {
            questionUpvoteRepository.delete(existingUpvote);
            question.setUpvoteCount(Math.max(0, defaultCount(question.getUpvoteCount()) - 1));
            questionRepository.save(question);
            return;
        }

        QuestionUpvote upvote = QuestionUpvote.builder()
                .questionId(questionId)
                .userId(userId)
                .build();
        questionUpvoteRepository.save(upvote);
        
        question.setUpvoteCount(defaultCount(question.getUpvoteCount()) + 1);
        questionRepository.save(question);
    }

    @Override
    @Transactional
    public void reportQuestion(String userId, String questionId, com.cinx.learning.community.application.command.CreateQnAReportCommand request) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));
        requireParticipant(userId, question.getCourseId());
        Report report = Report.builder()
                .refId(questionId)
                .type(ReportType.QUESTION)
                .reporterId(userId)
                .reason(request.getReason())
                .build();
        reportRepository.save(report);
    }

    @Override
    @Transactional
    public AnswerView createAnswer(String userId, String questionId, CreateAnswerCommand request) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        requireParticipant(userId, question.getCourseId());
        // every team member may answer; the instructor badge stays with owners and managers
        boolean instructorAnswer = isCourseInstructor(userId, question.getCourseId());

        CourseAnswer answer = mapper.toModel(request);
        answer.setUserId(userId);
        answer.setQuestionId(questionId);
        answer.setUpvoteCount(defaultCount(answer.getUpvoteCount()));
        answer.setIsInstructorAnswer(instructorAnswer);
        answer.setAiAssisted(Boolean.TRUE.equals(request.getAiAssisted()));

        String parentAuthorId = null;
        if (request.getParentAnswerId() != null) {
            CourseAnswer parent = answerRepository.findById(request.getParentAnswerId())
                    .orElseThrow(() -> new NotFoundException("Parent answer not found"));
            answer.setDepth(defaultCount(parent.getDepth()) + 1);
            parentAuthorId = parent.getUserId();
        } else {
            answer.setDepth(0);
        }

        answer = answerRepository.save(answer);

        CourseAnswerCreatedEvent event = CourseAnswerCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .answerId(answer.getId())
                .questionId(question.getId())
                .courseId(question.getCourseId())
                .questionAuthorId(question.getUserId())
                .parentAnswerAuthorId(parentAuthorId)
                .answeredByUserId(userId)
                .occurredAt(Instant.now())
                .build();
        eventPublisher.publishAnswerCreatedEvent(event);

        return withAuthor(mapAnswerToDtoWithRepliesCount(answer, userId));
    }

    @Override
    @Transactional
    public AnswerView updateAnswer(String userId, String answerId, UpdateAnswerCommand request) {
        CourseAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found"));
        if (!answer.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "Not the owner");
        }
        mapper.partialUpdate(answer, request);
        answer = answerRepository.save(answer);
        return withAuthor(mapAnswerToDtoWithRepliesCount(answer, userId));
    }

    @Override
    @Transactional
    public void deleteAnswer(String userId, String answerId) {
        CourseAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found"));
        if (!answer.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "Not the owner");
        }
        deleteAnswerTree(answer);
    }

    @Override
    @Transactional
    public void deleteAnswerByAdmin(String answerId) {
        CourseAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found"));
        deleteAnswerTree(answer);
    }

    private void deleteAnswerTree(CourseAnswer answer) {
        LinkedHashSet<String> answerIds = new LinkedHashSet<>();
        collectAnswerTreeIds(answer.getId(), answerIds);
        deleteAnswersByIds(answerIds);
    }

    private void collectAnswerTreeIds(String answerId, Set<String> answerIds) {
        if (!answerIds.add(answerId)) {
            return;
        }
        answerRepository.findByParentAnswerId(answerId).stream()
                .map(CourseAnswer::getId)
                .forEach(childAnswerId -> collectAnswerTreeIds(childAnswerId, answerIds));
    }

    private void deleteAnswersByIds(Iterable<String> answerIds) {
        List<String> ids = java.util.stream.StreamSupport.stream(answerIds.spliterator(), false)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        answerUpvoteRepository.deleteByAnswerIdIn(ids);
        answerHelpfulMarkRepository.deleteByAnswerIdIn(ids);
        ids.forEach(answerId -> reportRepository.deleteByRefIdAndType(answerId, ReportType.ANSWER));
        answerRepository.deleteByIdIn(ids);
    }

    @Override
    @Transactional
    public void upvoteAnswer(String userId, String answerId) {
        CourseAnswer answer = requireParticipantOfAnswer(userId, answerId);

        AnswerUpvote existingUpvote = answerUpvoteRepository.findByAnswerIdAndUserId(answerId, userId)
                .orElse(null);
        if (existingUpvote != null) {
            answerUpvoteRepository.delete(existingUpvote);
            answer.setUpvoteCount(Math.max(0, defaultCount(answer.getUpvoteCount()) - 1));
            answerRepository.save(answer);
            return;
        }

        AnswerUpvote upvote = AnswerUpvote.builder()
                .answerId(answerId)
                .userId(userId)
                .build();
        answerUpvoteRepository.save(upvote);

        answer.setUpvoteCount(defaultCount(answer.getUpvoteCount()) + 1);
        answerRepository.save(answer);
    }

    @Override
    @Transactional
    public void markAnswerHelpful(String userId, String answerId) {
        requireParticipantOfAnswer(userId, answerId);

        if (answerHelpfulMarkRepository.existsByAnswerIdAndUserId(answerId, userId)) {
            return;
        }

        answerHelpfulMarkRepository.save(AnswerHelpfulMark.builder()
                .answerId(answerId)
                .userId(userId)
                .build());
    }

    @Override
    @Transactional
    public void unmarkAnswerHelpful(String userId, String answerId) {
        requireParticipantOfAnswer(userId, answerId);
        answerHelpfulMarkRepository.deleteByAnswerIdAndUserId(answerId, userId);
    }

    @Override
    @Transactional
    public void reportAnswer(String userId, String answerId, com.cinx.learning.community.application.command.CreateQnAReportCommand request) {
        requireParticipantOfAnswer(userId, answerId);
        Report report = Report.builder()
                .refId(answerId)
                .type(ReportType.ANSWER)
                .reporterId(userId)
                .reason(request.getReason())
                .build();
        reportRepository.save(report);
    }

    private int defaultCount(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean defaultBoolean(Boolean value) {
        return value != null && value;
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
