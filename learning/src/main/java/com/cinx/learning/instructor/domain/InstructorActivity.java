package com.cinx.learning.instructor.domain;

import com.cinx.learning.shared.domain.ActivityAction;

import com.cinx.learning.shared.domain.InboxKind;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.time.LocalDateTime;

/** One action an instructor took on inbox work, however many items it covered. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorActivity extends Model {
    private String instructorId;
    private String courseId;
    private InboxKind kind;
    private ActivityAction action;
    private String targetId;
    private Integer itemCount;
    private String summary;
    private LocalDateTime createdAt;
}
