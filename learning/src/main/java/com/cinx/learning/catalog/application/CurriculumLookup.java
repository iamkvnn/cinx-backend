package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.SectionPlace;

import java.util.Collection;
import java.util.Map;

/** What the other features may ask the catalog about where lessons sit in a curriculum. */
public interface CurriculumLookup {
    /** The places of the sections with these ids, by section id; an id nobody knows is left out. */
    Map<String, SectionPlace> sectionPlaces(Collection<String> sectionIds);
}
