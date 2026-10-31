package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.application.query.UserStreakView;

public interface IStreakService {
    UserStreakView getUserStreak(String userId);
    void updateStreakOnActivity(String userId);
    /** Records that a milestone celebration, or the loss of the streak, has been shown. */
    UserStreakView markSeen(String userId, Integer milestone, boolean lost);
}
