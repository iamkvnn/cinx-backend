package com.cinx.learning.community.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReaction {
    private String id;
    private String userId;
    private String reviewId;
    private boolean liked;
}
