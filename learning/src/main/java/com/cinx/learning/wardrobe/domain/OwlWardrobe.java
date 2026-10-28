package com.cinx.learning.wardrobe.domain;

import com.cinx.learning.shared.domain.Model;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** What one learner's owl wears: a skin code per slot, null for the default look. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OwlWardrobe extends Model {
    private String userId;
    private String palette;
    private String head;
    private String face;
    private String effect;
    private String hold;
}
