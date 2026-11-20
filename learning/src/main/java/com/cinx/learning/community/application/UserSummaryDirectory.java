package com.cinx.learning.community.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.community.application.query.UserSummaryView;

import java.util.List;

/** Port: reads the display data of several users from the user service. */
public interface UserSummaryDirectory {
    ApiResponse<List<UserSummaryView>> getUsersByIds(List<String> ids);
}
