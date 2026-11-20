package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.application.query.UserStreakView;

public interface IStreakService {
    UserStreakView getUserStreak(String userId);
    void updateStreakOnActivity(String userId);
}
