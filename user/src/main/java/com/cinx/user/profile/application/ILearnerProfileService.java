package com.cinx.user.profile.application;

import com.cinx.user.profile.application.command.LearnerProfileAnswers;
import com.cinx.user.profile.application.command.SaveLearnerProfileCommand;
import com.cinx.user.profile.application.query.LearnerProfileView;

import java.util.List;

public interface ILearnerProfileService {
    /** The learner's profile, or an empty one (status NONE, consent on) when they have none yet. */
    LearnerProfileView get(String userId);

    LearnerProfileView save(String userId, SaveLearnerProfileCommand command);

    /** "Để sau" on the week page's invite: stops the invite; a completed profile is left alone. */
    LearnerProfileView skip(String userId);

    /** Writes what onboarding collected, in the onboarding transaction; nothing when it collected nothing. */
    void recordOnboarding(String userId, LearnerProfileAnswers answers, List<String> categoryIds);

    /** The topics from the older preferred-categories endpoint, kept with the rest of the profile. */
    void replaceCategories(String userId, List<String> categoryIds);
}
