package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.InstructorPublicView;

public interface IInstructorProfileService {
    /**
     * @throws com.cinx.common.exception.NotFoundException when the id is unknown, not an instructor, unverified or terminated
     */
    InstructorPublicView getPublicProfile(String instructorId);
}
