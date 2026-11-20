package com.cinx.learning.wardrobe.domain;

import java.util.List;
import java.util.Optional;

/** Port: where the owl skin catalog is stored. */
public interface OwlSkinRepository {
    List<OwlSkin> findAllByOrderBySortOrderAsc();

    Optional<OwlSkin> findByCode(String code);

    OwlSkin save(OwlSkin entity);
}
