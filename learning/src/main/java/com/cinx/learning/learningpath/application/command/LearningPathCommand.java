package com.cinx.learning.learningpath.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathCommand {
    private String title;
    private String description;
    private List<LearningPathItemCommand> items;
}
