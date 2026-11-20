package com.cinx.learning.review.domain;

import java.time.LocalDate;
import java.util.List;

/** Leitner v1: five boxes reviewed 1, 3, 7, 14 and 30 days apart; anything short of fully right starts over. */
public final class LeitnerSchedule {
    public static final String ALGORITHM = "LEITNER_V1";
    public static final int FIRST_BOX = 1;
    public static final int LAST_BOX = 5;
    private static final List<Integer> DAYS = List.of(1, 3, 7, 14, 30);

    private LeitnerSchedule() {
    }

    public static int intervalDays(int box) {
        if (box < FIRST_BOX || box > LAST_BOX) {
            throw new IllegalArgumentException("Leitner box out of range: " + box);
        }
        return DAYS.get(box - 1);
    }

    public static LocalDate dueOn(int box, LocalDate today) {
        return today.plusDays(intervalDays(box));
    }
}
