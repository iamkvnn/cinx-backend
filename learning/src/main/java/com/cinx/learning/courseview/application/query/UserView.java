package com.cinx.learning.courseview.application.query;

public record UserView(
         String userId, 
         String name, 
         String email, 
         String avatarUrl
) {
}