package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateQuizLessonCommand;
import com.cinx.learning.catalog.application.command.SyncQuizCommand;
import com.cinx.learning.catalog.application.command.UpdateQuizLessonCommand;

import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.query.QuizLessonView;
import com.cinx.learning.catalog.application.query.QuizQuestionView;
import com.cinx.learning.catalog.application.mapper.QuizMapper;
import com.cinx.learning.catalog.application.mapper.QuizQuestionMapper;
import com.cinx.learning.catalog.application.CourseEvents;
import com.cinx.learning.catalog.application.event.QuizSyncEvent;
import com.cinx.learning.catalog.application.event.ScoringModeChangedEvent;
import com.cinx.learning.catalog.domain.QuizLesson;
import com.cinx.learning.catalog.domain.QuizQuestion;
import com.cinx.learning.catalog.domain.QuizLessonRepository;
import com.cinx.learning.catalog.domain.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService implements IQuizService {
    private final QuizLessonRepository quizLessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final IQuizQuestionService quizQuestionService;
    private final ILessonService lessonService;
    private final ICourseAccessService courseAccessService;
    private final QuizMapper quizMapper;
    private final QuizQuestionMapper quizQuestionMapper;
    private final CourseEvents courseEventProducer;

    @Override
    @Transactional(readOnly = true)
    public QuizLessonView getQuizByLessonId(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.QUIZ);
        return forReader(currentUserId, courseId, getQuizOrThrow(lessonId));
    }

    @Override
    @Transactional(readOnly = true)
    public QuizLessonView getReadableQuizByLessonId(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.QUIZ);
        QuizLessonView quiz = getQuizOrThrow(lessonId);
        // Quiz sessions read the keys through getQuizByLessonId; this controller-facing read must not leak them.
        return courseAccessService.canManageCourse(currentUserId, courseId) ? quiz : quiz.withoutAnswerKeys();
    }

    /** An essay's reference answer is for the course team; a learner reading the same quiz never receives it. */
    private QuizLessonView forReader(String currentUserId, String courseId, QuizLessonView quiz) {
        return courseAccessService.canManageCourse(currentUserId, courseId) ? quiz : quiz.withoutReferenceAnswers();
    }

    private QuizLessonView getQuizOrThrow(String lessonId) {
        return quizLessonRepository.findByLessonId(lessonId)
                .map(quiz -> quizMapper.toDto(quiz, quizQuestionRepository.findAllByQuizLessonId(lessonId).stream().map(quizQuestionMapper::toDto).toList()))
                .orElseThrow(() -> new NotFoundException("Quiz not found for lessonId: " + lessonId));
    }

    @Transactional
    @Override
    public void createQuiz(String currentUserId, String courseId, String lessonId, CreateQuizLessonCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.QUIZ);
        if (request.getQuestions().size() < request.getNumberOfQuestionPerQuizSession()) {
            throw new BadRequestException(ErrorCode.QUIZ_CONFIGURATION_INVALID,
                    "Number of questions must be >= numberOfQuestionPerQuizSession");
        }

        quizLessonRepository.findByLessonId(lessonId).ifPresent(existing -> {
            throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Quiz already exists for lessonId: " + lessonId);
        });

        QuizLesson quizLesson = quizMapper.toModel(request);
        quizLesson.setLessonId(lessonId);
        QuizLesson saved = quizLessonRepository.save(quizLesson);

        quizQuestionService.addQuestions(saved.getLessonId(), request.getQuestions());
    }

    @Transactional
    @Override
    public void updateQuiz(String currentUserId, String courseId, String lessonId, UpdateQuizLessonCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.QUIZ);
        QuizLesson existing = getOrThrow(lessonId);

        boolean scoringModeChanged = request.getScoringMode() != null
                && !Objects.equals(existing.getScoringMode(), request.getScoringMode());

        if (request.getNumberOfQuestionPerQuizSession() != null) {
            int questionCount = quizQuestionRepository.countByQuizLessonId(existing.getLessonId());
            if (request.getNumberOfQuestionPerQuizSession() > questionCount) {
                throw new BadRequestException(ErrorCode.QUIZ_CONFIGURATION_INVALID,
                        "numberOfQuestionPerQuizSession (" + request.getNumberOfQuestionPerQuizSession()
                                + ") cannot exceed current question count (" + questionCount + ")");
            }
        }

        quizMapper.partialUpdate(existing, request);

        if (scoringModeChanged) {
            courseEventProducer.publishScoringModeChangedEvent(
                    new ScoringModeChangedEvent(existing.getLessonId(), existing.getScoringMode()));
        }

        quizLessonRepository.save(existing);
    }

    @Transactional
    @Override
    public void syncQuiz(String currentUserId, String courseId, String lessonId, SyncQuizCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.QUIZ);
        QuizLesson existing = getOrThrow(lessonId);

        if (Boolean.TRUE.equals(request.triggerRegrade())) {
            List<QuizQuestion> questions = quizQuestionRepository.findAllByQuizLessonIdAndNeedSync(lessonId);

            // the regrade consumer needs the answer keys, not an essay's reference answer
            List<QuizQuestionView> snapshots = questions.stream()
                    .map(quizQuestionMapper::toDto)
                    .map(QuizQuestionView::withoutReferenceAnswer)
                    .toList();

            QuizSyncEvent syncEvent = QuizSyncEvent.builder()
                    .quizLessonId(existing.getLessonId())
                    .scoringMode(existing.getScoringMode())
                    .changeReason(request.changeReason())
                    .questions(snapshots)
                    .build();

            courseEventProducer.publishQuizSyncEvent(syncEvent);
        }

        existing.setHasPendingSync(false);
        quizLessonRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizLesson getOrThrow(String lessonId) {
        return quizLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Quiz not found for lessonId: " + lessonId));
    }
}

