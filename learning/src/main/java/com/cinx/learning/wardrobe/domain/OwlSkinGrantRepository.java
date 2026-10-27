package com.cinx.learning.wardrobe.domain;

import java.util.List;

/** Port: where the season skins learners earned are stored. */
public interface OwlSkinGrantRepository {
    List<OwlSkinGrant> findByUserId(String userId);

    OwlSkinGrant save(OwlSkinGrant entity);
}
