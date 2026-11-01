package com.cinx.learning.featured.api.mapper;

import com.cinx.learning.featured.api.dto.request.AddFeaturedItemRequest;
import com.cinx.learning.featured.api.dto.request.ReorderRequest;
import com.cinx.learning.featured.api.dto.request.UpdateFeaturedItemRequest;
import com.cinx.learning.featured.api.dto.request.UpsertFeaturedPathRequest;
import com.cinx.learning.featured.api.dto.response.FeaturedCourseResponse;
import com.cinx.learning.featured.api.dto.response.FeaturedPathDetailResponse;
import com.cinx.learning.featured.api.dto.response.FeaturedPathResponse;
import com.cinx.learning.featured.api.dto.response.FeaturedReviewResponse;
import com.cinx.learning.featured.api.dto.response.PublicStatsResponse;
import com.cinx.learning.featured.application.command.AddFeaturedItemCommand;
import com.cinx.learning.featured.application.command.ReorderCommand;
import com.cinx.learning.featured.application.command.UpdateFeaturedItemCommand;
import com.cinx.learning.featured.application.command.UpsertFeaturedPathCommand;
import com.cinx.learning.featured.application.query.FeaturedCourseView;
import com.cinx.learning.featured.application.query.FeaturedPathDetailView;
import com.cinx.learning.featured.application.query.FeaturedPathView;
import com.cinx.learning.featured.application.query.FeaturedReviewView;
import com.cinx.learning.featured.application.query.PublicStatsView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface FeaturedApiMapper {
    AddFeaturedItemCommand toCommand(AddFeaturedItemRequest request);
    FeaturedCourseResponse toResponse(FeaturedCourseView view);
    FeaturedPathDetailResponse toResponse(FeaturedPathDetailView view);
    FeaturedPathResponse toResponse(FeaturedPathView view);
    FeaturedReviewResponse toResponse(FeaturedReviewView view);
    PublicStatsResponse toResponse(PublicStatsView view);
    ReorderCommand toCommand(ReorderRequest request);
    UpdateFeaturedItemCommand toCommand(UpdateFeaturedItemRequest request);
    UpsertFeaturedPathCommand toCommand(UpsertFeaturedPathRequest request);
    com.cinx.learning.featured.api.dto.response.FeaturedPathResponse.Course toResponse(com.cinx.learning.featured.application.query.FeaturedPathView.Course view);
    com.cinx.learning.featured.api.dto.response.FeaturedReviewResponse.Reviewer toResponse(com.cinx.learning.featured.application.query.FeaturedReviewView.Reviewer view);
    com.cinx.learning.featured.api.dto.response.FeaturedReviewResponse.CourseRef toResponse(com.cinx.learning.featured.application.query.FeaturedReviewView.CourseRef view);
}
