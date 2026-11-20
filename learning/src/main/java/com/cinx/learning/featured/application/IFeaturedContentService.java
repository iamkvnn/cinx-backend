package com.cinx.learning.featured.application;

import com.cinx.learning.featured.application.command.AddFeaturedItemCommand;
import com.cinx.learning.featured.application.command.ReorderCommand;
import com.cinx.learning.featured.application.command.UpdateFeaturedItemCommand;
import com.cinx.learning.featured.application.command.UpsertFeaturedPathCommand;
import com.cinx.learning.featured.application.query.FeaturedCourseView;
import com.cinx.learning.featured.application.query.FeaturedPathDetailView;
import com.cinx.learning.featured.application.query.FeaturedPathView;
import com.cinx.learning.featured.application.query.FeaturedReviewView;
import com.cinx.learning.featured.application.query.PublicStatsView;

import java.util.List;

public interface IFeaturedContentService {
    // public reads — only active items, only published courses
    List<FeaturedPathView> getPublicPaths();
    FeaturedPathDetailView getPublicPath(String id);
    List<FeaturedCourseView> getPublicCourses();
    List<FeaturedReviewView> getPublicReviews();
    PublicStatsView getPublicStats();

    // admin — every item, active or not
    List<FeaturedPathView> getAllPaths();
    FeaturedPathView createPath(UpsertFeaturedPathCommand request);
    FeaturedPathView updatePath(String id, UpsertFeaturedPathCommand request);
    void deletePath(String id);
    List<FeaturedPathView> reorderPaths(ReorderCommand request);

    List<FeaturedCourseView> getAllCourses();
    FeaturedCourseView addCourse(AddFeaturedItemCommand request);
    FeaturedCourseView updateCourse(String id, UpdateFeaturedItemCommand request);
    void removeCourse(String id);
    List<FeaturedCourseView> reorderCourses(ReorderCommand request);

    List<FeaturedReviewView> getAllReviews();
    FeaturedReviewView addReview(AddFeaturedItemCommand request);
    FeaturedReviewView updateReview(String id, UpdateFeaturedItemCommand request);
    void removeReview(String id);
    List<FeaturedReviewView> reorderReviews(ReorderCommand request);
}
