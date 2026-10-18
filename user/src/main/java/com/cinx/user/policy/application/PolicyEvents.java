package com.cinx.user.policy.application;

import java.time.LocalDateTime;

/** Port: tells the rest of the platform (the learning assistant's knowledge, for one) that a policy went live or was retired. */
public interface PolicyEvents {
    void publishPolicyPublished(String documentId, String title, String sourceUrl, String content,
                                Integer versionNumber, LocalDateTime publishedAt);

    void publishPolicyArchived(String documentId, String sourceUrl, LocalDateTime publishedAt);
}
