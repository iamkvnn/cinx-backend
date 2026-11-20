package com.cinx.learning.learningpath.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Replaces the lessons of an active or pending path, in this order. Title and description change only when sent. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathItemsUpdateCommand {
    private String title;
    private String description;
    private List<LearningPathItemCommand> items;
}
