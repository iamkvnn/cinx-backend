package com.cinx.learning.catalog.domain;

import java.util.List;
import java.util.Optional;

/** Port: where Section rows are stored. */
public interface SectionRepository {
    List<Section> findPublishedByCourse(String courseId);

    List<Section> findDraftByDraft(String draftId);

    List<Section> findDraftByDraftForUpdate(String draftId);

    Optional<Section> findDraftSection(String draftId, String stableId);

    void delete(Section entity);

    void deleteAll(Iterable<Section> entities);

    Optional<Section> findById(String id);

    List<Section> findAllById(Iterable<String> ids);

    Section save(Section entity);

    List<Section> saveAll(Iterable<Section> entities);
}
