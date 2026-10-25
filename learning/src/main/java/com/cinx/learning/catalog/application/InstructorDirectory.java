package com.cinx.learning.catalog.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.application.query.InstructorLookupView;
import com.cinx.learning.catalog.application.query.UserView;
import com.cinx.learning.shared.application.UpstreamCallException;

import java.util.List;

/**
 * Port: reads instructors from the user service.
 *
 * @throws UpstreamCallException from every method when the user service fails or answers 404
 */
public interface InstructorDirectory {
    ApiResponse<UserView> getInstructorById(String userId);

    ApiResponse<List<UserView>> getInstructorsByIds(List<String> ids);

    ApiResponse<Boolean> isInstructorVerified(String userId);

    /** Not found when no single verified instructor uses the email. */
    ApiResponse<InstructorLookupView> lookupInstructor(String email);
}
