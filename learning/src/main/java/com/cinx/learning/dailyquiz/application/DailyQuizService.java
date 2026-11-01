package com.cinx.learning.dailyquiz.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.dailyquiz.application.query.DailyQuizView;
import com.cinx.learning.dailyquiz.application.command.UpsertDailyQuizCommand;
import com.cinx.learning.dailyquiz.domain.DailyQuizOption;
import com.cinx.learning.dailyquiz.domain.DailyQuizQuestion;
import com.cinx.learning.dailyquiz.domain.DailyQuizQuestionRepository;
import com.cinx.learning.featured.application.command.ReorderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyQuizService implements IDailyQuizService {
    /** The home page is for Vietnamese learners, so "today" follows their calendar day. */
    static final ZoneId QUIZ_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DailyQuizQuestionRepository repository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public DailyQuizView getToday() {
        List<DailyQuizQuestion> active = repository.findByActiveTrueOrderBySortOrderAscIdAsc();
        if (active.isEmpty()) {
            throw new NotFoundException("No daily quiz question is configured");
        }
        LocalDate today = LocalDate.now(clock.withZone(QUIZ_ZONE));
        DailyQuizQuestion question = active.get(Math.floorMod(today.toEpochDay(), active.size()));
        return toResponse(question, false, today);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyQuizView> getAll() {
        return repository.findAllByOrderBySortOrderAsc().stream()
                .map(question -> toResponse(question, true, null))
                .toList();
    }

    @Override
    @Transactional
    public DailyQuizView create(UpsertDailyQuizCommand request) {
        List<DailyQuizOption> options = validatedOptions(request);
        DailyQuizQuestion saved = repository.save(DailyQuizQuestion.builder()
                .topic(request.topic().trim())
                .prompt(request.prompt().trim())
                .codeSnippet(blankToNull(request.codeSnippet()))
                .explanation(request.explanation().trim())
                .answerKey(request.answerKey())
                .active(request.active() == null || request.active())
                .sortOrder(repository.maxSortOrder() + 1)
                .options(options)
                .build());
        return toResponse(saved, true, null);
    }

    @Override
    @Transactional
    public DailyQuizView update(String id, UpsertDailyQuizCommand request) {
        DailyQuizQuestion question = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Daily quiz question not found with id: " + id));
        List<DailyQuizOption> options = validatedOptions(request);
        question.setTopic(request.topic().trim());
        question.setPrompt(request.prompt().trim());
        question.setCodeSnippet(blankToNull(request.codeSnippet()));
        question.setExplanation(request.explanation().trim());
        question.setAnswerKey(request.answerKey());
        if (request.active() != null) {
            question.setActive(request.active());
        }
        question.getOptions().clear();
        question.getOptions().addAll(options);
        return toResponse(repository.save(question), true, null);
    }

    @Override
    @Transactional
    public void delete(String id) {
        DailyQuizQuestion question = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Daily quiz question not found with id: " + id));
        repository.delete(question);
    }

    @Override
    @Transactional
    public List<DailyQuizView> reorder(ReorderCommand request) {
        List<DailyQuizQuestion> all = repository.findAll();
        Set<String> existing = all.stream().map(DailyQuizQuestion::getId).collect(Collectors.toSet());
        if (request.ids().size() != existing.size() || !existing.equals(new HashSet<>(request.ids()))) {
            throw new BadRequestException(ErrorCode.VALIDATION_FAILED, "Reorder must list every question exactly once");
        }
        Map<String, DailyQuizQuestion> byId = all.stream().collect(Collectors.toMap(DailyQuizQuestion::getId, Function.identity()));
        for (int i = 0; i < request.ids().size(); i++) {
            byId.get(request.ids().get(i)).setSortOrder(i);
        }
        repository.saveAll(all);
        return getAll();
    }

    /** Exactly the keys A–D once each, and the answer key must be one of them. */
    private static List<DailyQuizOption> validatedOptions(UpsertDailyQuizCommand request) {
        List<String> keys = request.options().stream().map(UpsertDailyQuizCommand.Option::key).toList();
        if (!new HashSet<>(keys).equals(Set.of("A", "B", "C", "D"))) {
            throw new BadRequestException(ErrorCode.QUIZ_CONFIGURATION_INVALID, "Options must use the keys A, B, C and D exactly once");
        }
        if (!keys.contains(request.answerKey())) {
            throw new BadRequestException(ErrorCode.QUIZ_CONFIGURATION_INVALID, "Answer key must match one of the options");
        }
        return request.options().stream()
                .sorted((a, b) -> a.key().compareTo(b.key()))
                .map(option -> new DailyQuizOption(option.key(), option.text().trim()))
                .collect(Collectors.toList());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static DailyQuizView toResponse(DailyQuizQuestion question, boolean admin, LocalDate day) {
        return new DailyQuizView(
                question.getId(),
                question.getTopic(),
                question.getPrompt(),
                question.getCodeSnippet(),
                question.getOptions().stream()
                        .map(option -> new DailyQuizView.Option(option.getOptionKey(), option.getOptionText()))
                        .toList(),
                question.getAnswerKey(),
                question.getExplanation(),
                admin ? question.getActive() : null,
                admin ? question.getSortOrder() : null,
                day);
    }
}
