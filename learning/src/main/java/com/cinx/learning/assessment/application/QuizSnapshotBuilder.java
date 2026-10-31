package com.cinx.learning.assessment.application;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.courseview.application.query.QuizOptionView;
import com.cinx.learning.courseview.application.query.QuizQuestionView;
import com.cinx.learning.assessment.application.query.QuizSessionOptionView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizSnapshotBuilder {

    private final ObjectMapper objectMapper;

    public String buildCorrectAnswer(QuizQuestionView q) {
        try {
            return switch (q.questionType()) {
                case ORDERING -> objectMapper.writeValueAsString(
                        q.options().stream()
                                .filter(o -> Boolean.TRUE.equals(o.isCorrect()))
                                .sorted(Comparator.comparing(QuizOptionView::optionOrder,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(QuizOptionView::id)
                                .toList());
                case MATCHING -> objectMapper.writeValueAsString(
                        q.options().stream()
                                .filter(o -> Boolean.TRUE.equals(o.isCorrect()))
                                .sorted(Comparator.comparing(QuizOptionView::optionOrder,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(o -> Map.of("optionId", o.id(), "matchText", o.matchText()))
                                .toList());
                case SHORT_TEXT, ESSAY -> objectMapper.writeValueAsString(
                        q.options().stream()
                                .filter(o -> Boolean.TRUE.equals(o.isCorrect()))
                                .map(QuizOptionView::optionText)
                                .sorted()
                                .toList());
                default -> // SINGLE_CHOICE, MULTI_CHOICE
                        objectMapper.writeValueAsString(
                                q.options().stream()
                                        .filter(o -> Boolean.TRUE.equals(o.isCorrect()))
                                        .map(QuizOptionView::id)
                                        .sorted()
                                        .toList());
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize correct answer to JSON", e);
        }
    }

    public String buildOptionsSnapshot(QuizQuestionView q, boolean isShuffle) {
        try {
            List<QuizOptionView> options = new ArrayList<>(q.options());
            if (isShuffle) {
                Collections.shuffle(options);
            }

            if (q.questionType() == QuizQuestionType.MATCHING) {
                List<QuizSessionOptionView> leftItems = options.stream()
                        .map(o -> new QuizSessionOptionView(
                                o.id(), o.optionText(), "LEFT"))
                        .toList();

                List<String> shuffledTexts = new ArrayList<>(options.stream()
                        .map(QuizOptionView::matchText)
                        .toList());
                Collections.shuffle(shuffledTexts);
                List<QuizSessionOptionView> rightItems = shuffledTexts.stream()
                        .map(text -> new QuizSessionOptionView(null, text, "RIGHT"))
                        .toList();

                List<QuizSessionOptionView> combined = new ArrayList<>(leftItems);
                combined.addAll(rightItems);
                return objectMapper.writeValueAsString(combined);
            }

            List<QuizSessionOptionView> items = new ArrayList<>(options.stream()
                    .map(o -> new QuizSessionOptionView(
                            o.id(), o.optionText(), null))
                    .toList());
            return objectMapper.writeValueAsString(items);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize options snapshot", e);
        }
    }

    public List<QuizSessionOptionView> parseOptionsSnapshot(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse optionsSnapshot: {}", e.getMessage());
            return List.of();
        }
    }
}
