package com.cinx.learning.shared.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.courseview.application.query.UserView;

/** Port: reads one user from the user service. */
public interface UserDirectory {
    /** @throws UpstreamCallException when the user service fails or does not know the id */
    ApiResponse<UserView> getUserById(String id);
}
