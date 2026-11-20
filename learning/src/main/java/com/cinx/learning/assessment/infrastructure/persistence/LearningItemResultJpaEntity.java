package com.cinx.learning.assessment.infrastructure.persistence;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "LearningItemResult")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LearningItemResultJpaEntity {
    @Id
    private String itemId;
    private Double score;
    private Integer weight;
}
