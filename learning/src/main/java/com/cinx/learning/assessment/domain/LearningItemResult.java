package com.cinx.learning.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningItemResult {
    private String itemId;
    private Double score;
    private Integer weight;
}
