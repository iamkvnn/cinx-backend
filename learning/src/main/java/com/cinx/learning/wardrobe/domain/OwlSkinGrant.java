package com.cinx.learning.wardrobe.domain;

import com.cinx.learning.shared.domain.Model;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/** A season skin a learner earned; kept after the season closes. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OwlSkinGrant extends Model {
    private String userId;
    private String skinCode;
    private LocalDateTime grantedAt;
}
