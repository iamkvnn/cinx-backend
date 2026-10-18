package com.cinx.user.policy.domain;

import java.util.List;
import java.util.Optional;

/** Port: where policy documents, with their sections, are stored. Lists are in the application's query port. */
public interface PolicyDocumentRepository {
    Optional<PolicyDocument> findById(String id);

    /** Stores the document and its sections and returns it as stored. */
    PolicyDocument save(PolicyDocument document);

    /** The highest version number any document of this slug has; 0 when there is none. */
    int findMaxVersionNumberBySlug(String slug);

    /** The published document of the slug with the highest version number. */
    Optional<PolicyDocument> findLatestPublishedBySlug(String slug);

    List<PolicyDocument> findAllPublishedBySlug(String slug);
}
