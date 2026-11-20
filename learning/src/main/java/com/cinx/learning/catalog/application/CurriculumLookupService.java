package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.SectionPlace;
import com.cinx.learning.catalog.domain.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurriculumLookupService implements CurriculumLookup {
    private final SectionRepository sections;

    @Override
    @Transactional(readOnly = true)
    public Map<String, SectionPlace> sectionPlaces(Collection<String> sectionIds) {
        Map<String, SectionPlace> places = new HashMap<>();
        if (sectionIds == null || sectionIds.isEmpty()) {
            return places;
        }
        sections.findAllById(new HashSet<>(sectionIds)).forEach(section -> places.put(section.getId(),
                new SectionPlace(section.getId(), section.getStableId(), section.getTitle(), section.getCourseId(), section.getDraftId())));
        return places;
    }
}
