package com.cinx.learning.featured.application.command;

import java.util.List;

/** The complete list of item ids in their new order; every existing item must appear exactly once. */
public record ReorderCommand(
        List<String> ids
) {
}
