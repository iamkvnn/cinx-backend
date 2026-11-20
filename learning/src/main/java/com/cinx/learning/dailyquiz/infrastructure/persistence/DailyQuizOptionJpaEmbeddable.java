package com.cinx.learning.dailyquiz.infrastructure.persistence;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyQuizOptionJpaEmbeddable {
    @Column(name = "option_key", length = 1, nullable = false)
    private String optionKey;

    @Column(name = "option_text", length = 200, nullable = false)
    private String optionText;
}
