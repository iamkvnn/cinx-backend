package com.cinx.learning.engagement.domain;

import java.util.Optional;

/** Port: where UserExperience rows are stored. */
public interface UserExperienceRepository {
    Optional<UserExperience> findById(String id);

    UserExperience save(UserExperience entity);
}
