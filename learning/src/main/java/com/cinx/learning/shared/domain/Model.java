package com.cinx.learning.shared.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A stored thing: its key, and the lock version the store keeps in step. A new one has neither until it is first stored. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class Model {
    private String id;
    private Long version;
}
