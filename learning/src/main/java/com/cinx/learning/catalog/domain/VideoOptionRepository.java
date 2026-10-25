package com.cinx.learning.catalog.domain;


/** Port: where VideoOption rows are stored. */
public interface VideoOptionRepository {
    void deleteAll(Iterable<VideoOption> entities);
}
