package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.domain.CourseAccessModel;
import com.cinx.learning.catalog.application.command.UpdateCourseAccessPolicyCommand;
import com.cinx.learning.catalog.application.query.CourseAccessPolicyView;
import com.cinx.learning.catalog.domain.CourseAccessPolicy;
import com.cinx.learning.catalog.domain.CourseAccessPolicyRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseAccessPolicyService {
    private final CourseAccessPolicyRepository repository;
    private final CourseRepository courses;
    private final Clock clock;

    @Transactional
    public CourseAccessPolicyView update(String courseId, UpdateCourseAccessPolicyCommand request) {
        if (request.accessModel() != CourseAccessModel.PURCHASE_ONLY
                && request.accessModel() != CourseAccessModel.SUBSCRIPTION_ONLY
                && request.accessModel() != CourseAccessModel.PURCHASE_OR_SUBSCRIPTION) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Unsupported course access model");
        }
        if ((request.accessModel() == CourseAccessModel.SUBSCRIPTION_ONLY
                || request.accessModel() == CourseAccessModel.PURCHASE_OR_SUBSCRIPTION)
                && (request.subscriptionFeatureKey() == null || request.subscriptionFeatureKey().isBlank())) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Subscription feature key is required");
        }
        if (request.accessModel() == CourseAccessModel.PURCHASE_ONLY
                && request.subscriptionFeatureKey() != null && !request.subscriptionFeatureKey().isBlank()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST,
                    "Subscription feature key is only valid for subscription-backed access models");
        }
        if ((request.accessModel() == CourseAccessModel.SUBSCRIPTION_ONLY
                || request.accessModel() == CourseAccessModel.PURCHASE_OR_SUBSCRIPTION)
                && !"learning.subscription-catalog.access".equals(request.subscriptionFeatureKey())) {
            throw new BadRequestException(ErrorCode.FEATURE_NOT_ENTITLED,
                    "Course policy references an unsupported subscription feature");
        }
        var course = courses.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        String actor = AuthenticationUtil.extractUserId();
        LocalDateTime now = LocalDateTime.now(clock);
        CourseAccessPolicy policy = repository.findById(courseId).orElseGet(() -> CourseAccessPolicy.builder()
                .courseId(courseId).effectiveFrom(now).version(0).build());
        policy.setAccessModel(request.accessModel());
        policy.setSubscriptionFeatureKey(request.subscriptionFeatureKey());
        policy.setEffectiveFrom(now);
        policy.setChangedBy(actor == null ? "system" : actor);
        policy.setReason(request.reason());
        course.setAccessModel(request.accessModel());
        course.setSubscriptionFeatureKey(request.subscriptionFeatureKey());
        // Keep the legacy event field derived from the policy during the
        // compatibility window; instructors never write this value.
        course.setIsInSubscription(request.accessModel() == CourseAccessModel.SUBSCRIPTION_ONLY
                || request.accessModel() == CourseAccessModel.PURCHASE_OR_SUBSCRIPTION);
        courses.save(course);
        return toResponse(repository.save(policy));
    }

    @Transactional(readOnly = true)
    public CourseAccessPolicyView get(String courseId) {
        return repository.findById(courseId).map(this::toResponse).orElse(null);
    }

    private CourseAccessPolicyView toResponse(CourseAccessPolicy policy) {
        return new CourseAccessPolicyView(policy.getCourseId(), policy.getAccessModel(),
                policy.getSubscriptionFeatureKey(), policy.getEffectiveFrom(), policy.getChangedBy(),
                policy.getReason(), policy.getVersion());
    }
}
