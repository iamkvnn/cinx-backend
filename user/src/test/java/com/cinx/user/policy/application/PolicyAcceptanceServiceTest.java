package com.cinx.user.policy.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.user.policy.application.query.PolicyQueries;
import com.cinx.user.policy.application.query.PolicySummaryView;
import com.cinx.user.policy.domain.PolicyAcceptance;
import com.cinx.user.policy.domain.PolicyAcceptanceRepository;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyAcceptanceServiceTest {
    @Mock PolicyQueries policyQueries;
    @Mock PolicyAcceptanceRepository acceptances;
    @InjectMocks PolicyAcceptanceService service;

    @Test
    void agreeingToEveryPublishedRequiredTypeReturnsThemWithTheirVersions() {
        when(policyQueries.findPublishedSummaries()).thenReturn(List.of(
                published("terms-1", PolicyType.TERMS, 3), published("privacy-1", PolicyType.PRIVACY, 2), published("cookie-1", PolicyType.COOKIE, 1)));

        List<AcceptedPolicy> accepted = service.resolveAccepted(List.of("terms-1", "privacy-1"));

        assertThat(accepted).containsExactlyInAnyOrder(
                new AcceptedPolicy("terms-1", 3, PolicyType.TERMS), new AcceptedPolicy("privacy-1", 2, PolicyType.PRIVACY));
    }

    @Test
    void idsAreTrimmedAndDeduplicatedAndBlanksIgnored() {
        when(policyQueries.findPublishedSummaries()).thenReturn(List.of(published("terms-1", PolicyType.TERMS, 1), published("privacy-1", PolicyType.PRIVACY, 1)));

        List<AcceptedPolicy> accepted = service.resolveAccepted(Arrays.asList(" terms-1 ", "terms-1", "", null, "privacy-1"));

        assertThat(accepted).extracting(AcceptedPolicy::policyId).containsExactlyInAnyOrder("terms-1", "privacy-1");
    }

    @Test
    void anIdThatIsNotPublishedIsRefused() {
        when(policyQueries.findPublishedSummaries()).thenReturn(List.of(published("terms-1", PolicyType.TERMS, 1)));

        assertThatThrownBy(() -> service.resolveAccepted(List.of("terms-1", "draft-or-unknown")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("published");
    }

    @Test
    void aPublishedRequiredTypeThatWasNotAgreedToIsRefusedByName() {
        when(policyQueries.findPublishedSummaries()).thenReturn(List.of(published("terms-1", PolicyType.TERMS, 1), published("privacy-1", PolicyType.PRIVACY, 1)));

        assertThatThrownBy(() -> service.resolveAccepted(List.of("terms-1")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("PRIVACY");
        assertThatThrownBy(() -> service.resolveAccepted(List.of()))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("TERMS");
        assertThatThrownBy(() -> service.resolveAccepted(null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void aRequiredTypeNobodyPublishedYetIsNotDemanded() {
        when(policyQueries.findPublishedSummaries()).thenReturn(List.of(published("terms-1", PolicyType.TERMS, 1)));

        assertThat(service.resolveAccepted(List.of("terms-1"))).hasSize(1);
    }

    @Test
    void nothingIsDemandedWhenNothingIsPublished() {
        when(policyQueries.findPublishedSummaries()).thenReturn(List.of());

        assertThat(service.resolveAccepted(null)).isEmpty();
    }

    @Test
    void recordStoresOneRowPerPolicyWithTheVersionAndTime() {
        LocalDateTime at = LocalDateTime.of(2026, 8, 1, 10, 0);

        service.record("user-1", List.of(new AcceptedPolicy("terms-1", 3, PolicyType.TERMS), new AcceptedPolicy("privacy-1", 2, PolicyType.PRIVACY)), at);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PolicyAcceptance>> captor = ArgumentCaptor.forClass(List.class);
        verify(acceptances).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(PolicyAcceptance::getUserId, PolicyAcceptance::getPolicyId, PolicyAcceptance::getPolicyVersion, PolicyAcceptance::getAcceptedAt)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("user-1", "terms-1", 3, at),
                        org.assertj.core.groups.Tuple.tuple("user-1", "privacy-1", 2, at));
    }

    private static PolicySummaryView published(String id, PolicyType type, int version) {
        return new PolicySummaryView(id, type, type.name().toLowerCase(), "Title", null, PolicyStatus.PUBLISHED, version, null, null, 0);
    }
}
