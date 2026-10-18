package com.cinx.user.policy.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** One headed part of a policy. A section that has not been stored yet has no id. */
@Getter
@Builder
@AllArgsConstructor
public class PolicySection {
    private final String id;
    private final String heading;
    private final String anchor;
    private final String bodyMarkdown;
    private final Integer orderIndex;
}
