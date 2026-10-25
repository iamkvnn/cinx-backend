package com.cinx.user.profile.domain;

import java.util.Optional;

/** Port: where learner profiles are stored. */
public interface LearnerProfileRepository {
    Optional<LearnerProfile> findByUserId(String userId);

    LearnerProfile save(LearnerProfile profile);
}
