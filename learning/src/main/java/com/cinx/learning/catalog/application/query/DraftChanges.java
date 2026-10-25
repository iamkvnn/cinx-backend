package com.cinx.learning.catalog.application.query;

import com.cinx.learning.shared.domain.LessonType;

import java.util.List;

/** The draft against the published version, matched by stable id. Lesson content is shared and live, so not compared. */
public record DraftChanges(
        boolean hasLiveVersion,
        List<FieldChange> course,
        SectionChanges sections,
        LessonChanges lessons
) {
    public static DraftChanges none(boolean hasLiveVersion) {
        return new DraftChanges(hasLiveVersion, List.of(),
                new SectionChanges(List.of(), List.of(), List.of(), List.of()),
                new LessonChanges(List.of(), List.of(), List.of()));
    }

    /** {@code before} is the published value, {@code after} the draft's. */
    public record FieldChange(String field, Object before, Object after) {
    }

    public record SectionRef(String sectionId, String title) {
    }

    public record SectionChange(String sectionId, String before, String after) {
    }

    /** {@code moved} lists the sections that left their place; the others shifted around them. */
    public record SectionChanges(List<SectionRef> added, List<SectionRef> removed,
                                 List<SectionChange> renamed, List<SectionRef> moved) {
    }

    public record LessonRef(String lessonId, String title, String sectionTitle, LessonType type) {
    }

    /** MOVED: another section. REORDERED: left its place inside the same section. */
    public enum LessonChangeKind {
        TITLE, DURATION, PREVIEW, PREREQUISITES, MOVED, REORDERED
    }

    public record LessonChange(String lessonId, String title, String sectionTitle, List<LessonChangeKind> changes) {
    }

    public record LessonChanges(List<LessonRef> added, List<LessonRef> removed, List<LessonChange> changed) {
    }
}
