package com.cinx.learning.assessment.application.command;

import java.util.List;

/** The full, ordered list of an assignment's criteria; an empty list removes them all. */
public record SaveCriteriaCommand(
           List<CriterionInput> criteria
) {
    public record CriterionInput(
            String id,
              String title,
             String description,
               Double maxPoints
    ) {
    }
}
