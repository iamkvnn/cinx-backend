package com.cinx.learning.instructor.infrastructure.persistence;

import com.cinx.learning.shared.domain.ActivityAction;
import com.cinx.learning.shared.domain.InboxKind;

import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/** One action an instructor took on inbox work, however many items it covered. */
@Entity(name = "InstructorActivity")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorActivityJpaEntity extends BaseEntity {
    @Column(nullable = false)
    private String instructorId;

    @Column(nullable = false)
    private String courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InboxKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityAction action;

    private String targetId;

    @Column(nullable = false)
    private Integer itemCount;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
