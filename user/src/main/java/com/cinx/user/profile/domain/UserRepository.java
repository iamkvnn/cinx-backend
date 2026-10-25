package com.cinx.user.profile.domain;

import java.util.List;
import java.util.Optional;

/** Port: where profiles are stored. Lists and counts are in the application's query port. */
public interface UserRepository {
    Optional<User> findByUserId(String userId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** user.email has no unique index, so a lookup must handle several rows. */
    List<User> findAllByEmailIgnoreCase(String email);

    List<User> findAllByUserIdIn(List<String> userIds);

    /** Stores the profile and returns it as stored, with its key and timestamps. */
    User save(User user);
}
