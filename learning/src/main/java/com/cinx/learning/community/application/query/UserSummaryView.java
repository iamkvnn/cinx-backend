package com.cinx.learning.community.application.query;

public record UserSummaryView(
        String userId,
        String name,
        String email,
        String role,
        String avatarUrl
) {
    /** Public reads (the course page's review list) never carry an email. */
    public UserSummaryView withoutEmail() {
        return new UserSummaryView(userId, name, null, role, avatarUrl);
    }
}
