package com.cinx.user.profile.infrastructure.persistence;

import com.cinx.user.profile.domain.ContentLanguage;
import com.cinx.user.profile.domain.LearnerLevel;
import com.cinx.user.profile.domain.LearnerProfileStatus;
import com.cinx.user.profile.domain.LearningGoal;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "learner_profile")
@Getter
@Setter
@NoArgsConstructor
class LearnerProfileJpaEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private LearningGoal goal;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LearnerLevel level;

    @Column(name = "minutes_per_day")
    private Integer minutesPerDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_language", length = 10)
    private ContentLanguage contentLanguage;

    @Column(name = "personalization_consent", nullable = false)
    private boolean personalizationConsent;

    @Column(name = "usage_analytics_consent", nullable = false)
    private boolean usageAnalyticsConsent = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearnerProfileStatus status;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "learner_profile_category", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category_id", nullable = false)
    private Set<String> categoryIds = new LinkedHashSet<>();
}
