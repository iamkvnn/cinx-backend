package com.cinx.learning.shared;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssistedContentMigrationContractTest {
    @Test
    void migrationAddsAFalseByDefaultAiLabelToEveryLabelledTable() throws Exception {
        String migration = Files.readString(Path.of(
                "src", "main", "resources", "db", "migration", "V28__ai_assisted_content.sql"));

        for (String table : new String[]{"course_answers", "quiz_question", "quiz_session_question", "course", "course_draft"}) {
            assertThat(migration).contains("ALTER TABLE " + table + " ADD COLUMN ai_assisted BOOLEAN NOT NULL DEFAULT FALSE;");
        }
    }
}
