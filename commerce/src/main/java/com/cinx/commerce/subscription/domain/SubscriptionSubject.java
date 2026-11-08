package com.cinx.commerce.subscription.domain;

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
public class SubscriptionSubject {
    private String id;
    private String subjectType;
    private String subjectReference;
    private LocalDateTime createdAt;
}
