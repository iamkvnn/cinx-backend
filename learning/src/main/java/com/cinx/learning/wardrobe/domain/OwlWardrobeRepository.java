package com.cinx.learning.wardrobe.domain;

import java.util.Optional;

/** Port: where each learner's owl wardrobe is stored. */
public interface OwlWardrobeRepository {
    Optional<OwlWardrobe> findByUserId(String userId);

    OwlWardrobe save(OwlWardrobe entity);
}
