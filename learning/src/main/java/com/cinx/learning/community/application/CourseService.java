package com.cinx.learning.community.application;


import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.application.ICourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("communityCourseService")
@RequiredArgsConstructor
public class CourseService {
    private final ICourseService courseService;

    @Transactional
    public ApiResponse<Void> updateCourseRating(String id, Double rating) {
        courseService.updateCourseRating(id, rating);
        return ApiResponse.success("Course rating updated", null);
    }

    @Transactional
    public ApiResponse<Void> updateCourseRatingStats(String id, Double rating, long ratingCount) {
        courseService.updateCourseRatingStats(id, rating, ratingCount);
        return ApiResponse.success("Course rating updated", null);
    }
}
