package com.cinx.learning.catalog.application.query;

/** Where a section sits: its titles and the course (published) or the draft it belongs to. */
public record SectionPlace(String id, String stableId, String title, String courseId, String draftId) {
}
