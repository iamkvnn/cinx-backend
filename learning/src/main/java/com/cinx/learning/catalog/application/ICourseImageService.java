package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateCourseImageCommand;
import com.cinx.learning.catalog.application.command.ReorderCourseImagesCommand;
import com.cinx.learning.catalog.application.command.UpdateCourseImageCommand;
import com.cinx.learning.catalog.application.query.CourseImageView;

import java.util.List;

public interface ICourseImageService {
    void saveCourseImages(String currentUserId, String courseId, CreateCourseImageCommand request);
    void updateCourseImage(String currentUserId, String courseId, String imageId, UpdateCourseImageCommand request);
    void deleteImage(String currentUserId, String courseId, String imageId);
    List<CourseImageView> reorderImages(String currentUserId, String courseId, ReorderCourseImagesCommand request);
}

