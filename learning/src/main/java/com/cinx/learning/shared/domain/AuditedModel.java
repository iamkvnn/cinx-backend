package com.cinx.learning.shared.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/** A stored thing that also records who created and last changed it, and when. The store stamps these; they are read-only here. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AuditedModel extends Model {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
