package com.cinx.learning.community;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerHelpfulMarkMigrationContractTest {
    @Test
    void migrationCreatesPrivatePerUserAnswerMarksWithRequiredIndexes() throws Exception {
        String migration = Files.readString(Path.of(
                "src", "main", "resources", "db", "migration", "V11__answer_helpful_marks.sql"));

        assertThat(migration)
                .contains("CREATE TABLE IF NOT EXISTS answer_helpful_marks")
                .contains("answer_id VARCHAR(255) NOT NULL")
                .contains("user_id VARCHAR(255) NOT NULL")
                .contains("UNIQUE KEY uk_answer_helpful_marks_answer_user (answer_id, user_id)")
                .contains("KEY idx_answer_helpful_marks_user (user_id)");
    }
}
