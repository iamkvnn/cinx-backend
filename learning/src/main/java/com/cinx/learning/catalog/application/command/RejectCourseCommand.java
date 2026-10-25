package com.cinx.learning.catalog.application.command;

import java.util.List;

/** A rejection carries an overall reason, the reviewer's drafted notes, or both; at least one is required. */
public record RejectCourseCommand(
        String reason,
        List<String> noteIds
) {
}
