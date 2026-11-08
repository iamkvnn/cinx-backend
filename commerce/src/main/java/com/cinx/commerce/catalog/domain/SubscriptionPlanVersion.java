package com.cinx.commerce.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanVersion {
    private String id;
    private String planId;
    private String billingPolicyVersionId;
    private long versionNumber;
    private String publicationStatus;
    private String defaultLocale;
    private LocalDateTime effectiveFrom;
    private LocalDateTime publishedAt;
    private String publishedBy;
    private long optimisticVersion;
}
