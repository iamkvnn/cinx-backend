package com.cinx.learning.community.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewCreatedEvent {
    private String eventId;
    private String reviewId;
    private String courseId;
    private String reviewerUserId;
    private Double rating;
    private String content;
    private Instant occurredAt;
}
