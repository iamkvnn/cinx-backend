package com.cinx.learning.catalog.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity(name = "AssignmentLesson")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentLessonJpaEntity {
    @Id
    private String lessonId;
    private String description;

    @OneToMany(mappedBy = "assignmentLesson", cascade = CascadeType.ALL)
    private List<AssignmentAttachmentJpaEntity> attachments;
}

