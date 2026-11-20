package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.application.command.CreateQuizQuestionCommand;
import com.cinx.learning.catalog.application.command.UpdateQuizOptionCommand;
import com.cinx.learning.catalog.application.command.UpdateQuizQuestionCommand;
import com.cinx.learning.catalog.application.query.QuizQuestionView;
import com.cinx.learning.catalog.application.mapper.QuizQuestionMapper;
import com.cinx.learning.catalog.domain.QuizLesson;
import com.cinx.learning.catalog.domain.QuizOption;
import com.cinx.learning.catalog.domain.QuizQuestion;
import com.cinx.learning.catalog.domain.QuizLessonRepository;
import com.cinx.learning.catalog.domain.QuizOptionRepository;
import com.cinx.learning.catalog.domain.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizQuestionService implements IQuizQuestionService {
    private final QuizQuestionMapper quizQuestionMapper;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final QuizLessonRepository quizLessonRepository;
    private final ILessonService lessonService;
    private final ICourseAccessService courseAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestionView> getQuestions(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.QUIZ);
        quizLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Quiz not found for lessonId: " + lessonId));
        boolean courseTeam = courseAccessService.canManageCourse(currentUserId, courseId);
        return quizQuestionRepository.findAllByQuizLessonId(lessonId).stream()
                .map(quizQuestionMapper::toDto)
                // answer keys and an essay's reference answer are for the course team; a learner never receives them
                .map(question -> courseTeam ? question : question.withoutAnswerKeys())
                .toList();
    }

    @Transactional
    @Override
    public QuizQuestionView addQuestion(String currentUserId, String courseId, String lessonId, CreateQuizQuestionCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.QUIZ);
        QuizLesson quiz = quizLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Quiz not found for lessonId: " + lessonId));

        QuizQuestion newQuestion = createQuestionEntity(quiz, request);
        QuizQuestion savedQuestion = quizQuestionRepository.save(newQuestion);
        List<QuizOption> options = newQuestion.getOptions();
        options.forEach(o -> o.setQuizQuestionId(savedQuestion.getId()));
        savedQuestion.setOptions(quizOptionRepository.saveAll(options));

        return quizQuestionMapper.toDto(savedQuestion);
    }

    private QuizQuestion createQuestionEntity(QuizLesson lesson, CreateQuizQuestionCommand request) {
        boolean essay = request.questionType() == QuizQuestionType.ESSAY;
        if (!essay && (request.options() == null || request.options().isEmpty())) {
            throw new BadRequestException("A " + request.questionType() + " question needs at least one option");
        }
        return QuizQuestion.builder()
                .questionText(request.questionText())
                .questionType(request.questionType())
                .scoringMethod(request.scoringMethod())
                .referenceAnswer(essay ? blankToNull(request.referenceAnswer()) : null)
                .explanation(blankToNull(request.explanation()))
                .aiAssisted(Boolean.TRUE.equals(request.aiAssisted()))
                .quizLessonId(lesson.getLessonId())
                // an essay is graded by hand and stores no options
                .options(essay ? List.<QuizOption>of() : request.options()
                        .stream()
                        .<QuizOption>map(o -> QuizOption.builder()
                                .optionText(o.optionText())
                                .isCorrect(o.isCorrect())
                                .optionOrder(o.optionOrder())
                                .matchText(o.matchText())
                                .build()
                        ).toList()
                )
                .build();
    }

    @Transactional
    @Override
    public List<QuizQuestionView> addQuestions(String lessonId, List<CreateQuizQuestionCommand> requests) {
        QuizLesson quiz = quizLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Quiz not found for lessonId: " + lessonId));

        List<QuizQuestion> newQuestions = requests.stream()
                .map(req -> createQuestionEntity(quiz, req))
                .toList();
        List<QuizQuestion> savedQuestions = quizQuestionRepository.saveAll(newQuestions);

        // saveAll answers in the order it was given, so the options of the i-th new question belong to the i-th stored one
        List<QuizOption> allOptions = new ArrayList<>();
        for (int i = 0; i < savedQuestions.size(); i++) {
            String questionId = savedQuestions.get(i).getId();
            newQuestions.get(i).getOptions().forEach(o -> {
                o.setQuizQuestionId(questionId);
                allOptions.add(o);
            });
        }
        Map<String, List<QuizOption>> optionMap = quizOptionRepository.saveAll(allOptions).stream()
                .collect(Collectors.groupingBy(o -> o.getQuizQuestionId()));
        savedQuestions.forEach(q -> q.setOptions(optionMap.getOrDefault(q.getId(), Collections.emptyList())));

        return savedQuestions.stream().map(quizQuestionMapper::toDto).toList();
    }

    @Transactional
    @Override
    public QuizQuestionView updateQuestion(String currentUserId, String courseId, String lessonId, String questionId, UpdateQuizQuestionCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.QUIZ);
        QuizLesson quiz = quizLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Quiz not found for lessonId: " + lessonId));
        QuizQuestion question = quizQuestionRepository.findByIdAndQuizLessonId(questionId, lessonId)
                .orElseThrow(() -> new NotFoundException("Question not found: " + questionId));

        boolean essay = question.getQuestionType() == QuizQuestionType.ESSAY;
        if (!essay && (request.options() == null || request.options().isEmpty())) {
            throw new BadRequestException("A " + question.getQuestionType() + " question needs at least one option");
        }

        question.setQuestionText(request.questionText());
        question.setScoringMethod(request.scoringMethod());
        question.setReferenceAnswer(essay ? blankToNull(request.referenceAnswer()) : null);
        question.setExplanation(blankToNull(request.explanation()));
        question.setNeedSync(true);

        quizQuestionRepository.save(question);
        mergeOptions(question, essay ? List.of() : request.options());

        quiz.setHasPendingSync(true);
        quizLessonRepository.save(quiz);

        return quizQuestionMapper.toDto(question);
    }

    @Transactional
    @Override
    public void deleteQuestion(String currentUserId, String courseId, String lessonId, String questionId) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.QUIZ);
        QuizLesson quiz = quizLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Quiz not found for lessonId: " + lessonId));
        QuizQuestion question = quizQuestionRepository.findByIdAndQuizLessonId(questionId, lessonId)
                .orElseThrow(() -> new NotFoundException("Question not found: " + questionId));

        int currentCount = quizQuestionRepository.countByQuizLessonId(lessonId);
        Integer minRequired = quiz.getNumberOfQuestionPerQuizSession();
        if (minRequired != null && currentCount <= minRequired) {
            throw new BadRequestException(ErrorCode.QUIZ_CONFIGURATION_INVALID, "Quiz must have at least " + minRequired + " question(s)");
        }

        quizOptionRepository.deleteAllByQuizQuestionId(questionId);
        quizQuestionRepository.delete(question);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void mergeOptions(QuizQuestion question, List<UpdateQuizOptionCommand> incomingOptions) {
        List<QuizOption> existingOptions = question.getOptions();

        Map<String, QuizOption> existingById = existingOptions.stream()
                .collect(Collectors.toMap(QuizOption::getId, o -> o));
        Set<String> incomingIds = incomingOptions.stream()
                .map(UpdateQuizOptionCommand::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        quizOptionRepository.deleteAllById(existingOptions.stream()
                .map(QuizOption::getId)
                .filter(id -> !incomingIds.contains(id))
                .toList());

        List<QuizOption> optionsToSave = new ArrayList<>();
        incomingOptions.forEach(o -> {
            QuizOption option;
            if (o.id() != null) {
                option = existingById.get(o.id());
                if (option == null) {
                    throw new NotFoundException("Option not found: " + o.id());
                }
            } else {
                option = new QuizOption();
                option.setQuizQuestionId(question.getId());
            }
            option.setOptionText(o.optionText());
            option.setIsCorrect(o.isCorrect());
            option.setOptionOrder(o.optionOrder());
            option.setMatchText(o.matchText());
            optionsToSave.add(option);
        });
        question.setOptions(quizOptionRepository.saveAll(optionsToSave));
    }
}

