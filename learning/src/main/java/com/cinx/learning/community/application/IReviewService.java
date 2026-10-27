package com.cinx.learning.community.application;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.community.application.command.CreateReportReviewCommand;
import com.cinx.learning.community.application.command.CreateReviewReactionCommand;
import com.cinx.learning.community.application.command.CreateReviewCommand;
import com.cinx.learning.community.application.command.UpdateReviewCommand;
import com.cinx.learning.community.application.query.ReviewView;

import com.cinx.learning.community.application.command.CreateReviewReplyCommand;
import com.cinx.learning.community.application.command.UpdateReviewReplyCommand;

public interface IReviewService {
    PageResult<ReviewView> getReviewsByCourseId(String courseId, int page, int size, String sort, String currentUserId, boolean unreplied);
    ReviewView getReviewById(String reviewId);
    void createReview(String userId, CreateReviewCommand request);
    void updateReview(String userId, String reviewId, UpdateReviewCommand request);
    void deleteReview(String userId, String reviewId);
    void deleteReviewByAdmin(String reviewId);
    void reportReview(String userId, String reviewId, CreateReportReviewCommand request);
    void reactReview(String userId, String reviewId, CreateReviewReactionCommand request);
    
    void createReviewReply(String userId, String reviewId, CreateReviewReplyCommand request);
    void updateReviewReply(String userId, String replyId, UpdateReviewReplyCommand request);
    void deleteReviewReply(String userId, String replyId);
}
