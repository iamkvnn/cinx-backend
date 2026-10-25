package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.UpdateCoursePromoVideoCommand;

public interface ICoursePromoVideoService {
    /** Sets or replaces the course's intro video. It applies at once, like the images. */
    void putPromoVideo(String currentUserId, String courseId, UpdateCoursePromoVideoCommand request);

    void removePromoVideo(String currentUserId, String courseId);
}
