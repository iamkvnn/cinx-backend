package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateSectionCommand;
import com.cinx.learning.catalog.application.command.MoveSectionCommand;
import com.cinx.learning.catalog.application.command.UpdateSectionCommand;
import com.cinx.learning.catalog.application.query.SectionPositionView;
import com.cinx.learning.catalog.application.query.SectionView;
import com.cinx.learning.catalog.domain.Section;

public interface ISectionService {
    SectionView createSection(String currentUserId, String courseId, CreateSectionCommand request);
    SectionView updateSection(String currentUserId, String courseId, String sectionId, UpdateSectionCommand request);
    SectionPositionView moveSection(String currentUserId, String courseId, String sectionId, MoveSectionCommand request);
    void deleteSection(String currentUserId, String courseId, String sectionId);
    Section editableSection(String courseId, String sectionId);
}

