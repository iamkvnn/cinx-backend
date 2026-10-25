package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VideoQuestion extends Model {
    private String questionText;
    private VideoQuizQuestionType questionType;
    private Integer timestampSeconds;
    @Builder.Default
    private List<VideoOption> options = new ArrayList<>();
    private String videoLessonId;
}
