package com.cinx.notification.notification.application.query;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import java.util.List;

/** Port for the inbox reads. They return read models, so no aggregate is loaded to answer a list or a count. */
public interface NotificationQueries {
    /**
     * The caller's rows, newest first unless the page names another sort; {@code query}, {@code isRead} and {@code type}
     * are optional and combine.
     */
    PageResult<UserNotificationView> findPage(String userId, String query, Boolean isRead, String type, PageQuery page);

    long countUnread(String userId);

    List<TypeCount> countsByType(String userId);
}
