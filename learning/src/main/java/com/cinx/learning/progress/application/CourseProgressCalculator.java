package com.cinx.learning.progress.application;

import com.cinx.learning.progress.domain.CourseProgressAggregate;

import com.cinx.learning.progress.domain.LearningItemProgress;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CourseProgressCalculator {

    public CourseProgressAggregate calculate(List<LearningItemProgress> items, Integer expectedTotalItems) {
        int totalItems = expectedTotalItems != null ? expectedTotalItems : items.size();
        long completedItems = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsCompleted()))
                .count();
        long passedItems = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsCompleted()))
                .filter(item -> Boolean.TRUE.equals(item.getIsPassed()))
                .count();
        double avgScore = completedItems > 0
                ? items.stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsCompleted()))
                        .mapToDouble(item -> item.getScore() != null ? item.getScore() : 0.0)
                        .average()
                        .orElse(0.0)
                : 0.0;
        boolean completed = totalItems > 0 && completedItems == totalItems;
        boolean passed = completed && passedItems == totalItems;

        return new CourseProgressAggregate(totalItems, (int) completedItems, avgScore, completed, passed);
    }
}
