package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.application.query.PlanFeatureGrant;
import com.cinx.commerce.catalog.application.query.PlanVersionRef;
import com.cinx.commerce.catalog.application.query.SaleableOffer;
import com.cinx.commerce.catalog.domain.PlanCatalogRepository;
import com.cinx.commerce.catalog.domain.PlanEntitlement;
import com.cinx.commerce.catalog.domain.PlanOffer;
import com.cinx.commerce.catalog.domain.PlanPrice;
import com.cinx.commerce.catalog.domain.SubscriptionPlan;
import com.cinx.commerce.catalog.domain.SubscriptionPlanVersion;
import com.cinx.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanCatalogLookupServiceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock PlanCatalogRepository catalog;
    PlanCatalogLookupService service;

    @BeforeEach
    void setUp() {
        service = new PlanCatalogLookupService(catalog);
        lenient().when(catalog.findVersionById("v1")).thenReturn(Optional.of(
                SubscriptionPlanVersion.builder().id("v1").planId("p1").versionNumber(2).publicationStatus("PUBLISHED").build()));
        lenient().when(catalog.findPlanById("p1")).thenReturn(Optional.of(
                SubscriptionPlan.builder().id("p1").code("LEARNER_PLUS").lifecycleStatus("ACTIVE").build()));
        lenient().when(catalog.findPrice("price-1", "p1")).thenReturn(Optional.of(PlanPrice.builder().id("price-1").planId("p1")
                .amount(new BigDecimal("99000.00")).currency("VND").status("RECONCILED").providerPriceRef("price_stripe_1").build()));
    }

    @Test
    void aPaidOfferOfAPublishedActivePlanWithAReconciledPriceIsSaleable() {
        when(catalog.findOfferById("o1")).thenReturn(Optional.of(offer("o1", "price-1", NOW.minusDays(1), NOW.plusDays(1))));

        assertThat(service.resolveSaleableOffer("o1", NOW)).isEqualTo(new SaleableOffer("o1", "price-1"));
    }

    @Test
    void aFreeOfferNeedsNoPrice() {
        when(catalog.findOfferById("o1")).thenReturn(Optional.of(offer("o1", null, null, null)));

        assertThat(service.resolveSaleableOffer("o1", NOW)).isEqualTo(new SaleableOffer("o1", null));
    }

    @Test
    void theSalesWindowStartsInclusiveAndEndsExclusive() {
        when(catalog.findOfferById("start")).thenReturn(Optional.of(offer("start", null, NOW, NOW.plusDays(1))));
        when(catalog.findOfferById("end")).thenReturn(Optional.of(offer("end", null, NOW.minusDays(1), NOW)));

        assertThat(service.resolveSaleableOffer("start", NOW).offerId()).isEqualTo("start");
        assertThatThrownBy(() -> service.resolveSaleableOffer("end", NOW)).hasMessageContaining("not currently saleable");
        assertThatThrownBy(() -> service.resolveSaleableOffer("start", NOW.minusSeconds(1))).hasMessageContaining("not currently saleable");
    }

    @Test
    void aWindowThatEndsBeforeItStartsIsInvalid() {
        when(catalog.findOfferById("o1")).thenReturn(Optional.of(offer("o1", null, NOW.plusDays(1), NOW.minusDays(1))));

        assertThatThrownBy(() -> service.resolveSaleableOffer("o1", NOW)).hasMessageContaining("window is invalid");
    }

    @Test
    void anOfferThatIsMissingOrNotActiveIsUnavailable() {
        PlanOffer retired = offer("old", null, null, null);
        retired.setStatus("RETIRED");
        when(catalog.findOfferById("old")).thenReturn(Optional.of(retired));
        when(catalog.findOfferById("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveSaleableOffer("old", NOW)).isInstanceOf(ForbiddenException.class).hasMessageContaining("unavailable");
        assertThatThrownBy(() -> service.resolveSaleableOffer("gone", NOW)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void anOfferOnADraftVersionOrARetiredPlanIsNotSaleable() {
        when(catalog.findOfferById("o1")).thenReturn(Optional.of(offer("o1", null, null, null)));
        when(catalog.findVersionById("v1")).thenReturn(Optional.of(
                SubscriptionPlanVersion.builder().id("v1").planId("p1").publicationStatus("DRAFT").build()));
        assertThatThrownBy(() -> service.resolveSaleableOffer("o1", NOW)).hasMessageContaining("published plan");

        when(catalog.findVersionById("v1")).thenReturn(Optional.of(
                SubscriptionPlanVersion.builder().id("v1").planId("p1").publicationStatus("PUBLISHED").build()));
        when(catalog.findPlanById("p1")).thenReturn(Optional.of(
                SubscriptionPlan.builder().id("p1").code("X").lifecycleStatus("RETIRED").build()));
        assertThatThrownBy(() -> service.resolveSaleableOffer("o1", NOW)).hasMessageContaining("Plan is not active");
    }

    @Test
    void aPaidOfferWhosePriceIsNotReconciledWithTheProviderIsNotSaleable() {
        when(catalog.findOfferById("o1")).thenReturn(Optional.of(offer("o1", "price-1", null, null)));
        when(catalog.findPrice("price-1", "p1")).thenReturn(Optional.of(PlanPrice.builder().id("price-1").planId("p1")
                .amount(new BigDecimal("99000.00")).currency("VND").status("DRAFT").providerPriceRef(null).build()));

        assertThatThrownBy(() -> service.resolveSaleableOffer("o1", NOW)).hasMessageContaining("not been reconciled");
    }

    @Test
    void anOfferWhosePriceBelongsToAnotherPlanIsUnavailable() {
        when(catalog.findOfferById("o1")).thenReturn(Optional.of(offer("o1", "price-9", null, null)));
        when(catalog.findPrice("price-9", "p1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveSaleableOffer("o1", NOW)).hasMessageContaining("price is unavailable");
    }

    @Test
    void theCodeOfAVersionsPlanIsLookedUpThroughTheVersion() {
        assertThat(service.planCodeOfVersion("v1")).contains("LEARNER_PLUS");
        when(catalog.findVersionById("none")).thenReturn(Optional.empty());
        assertThat(service.planCodeOfVersion("none")).isEmpty();
    }

    @Test
    void aVersionIsFoundByThePlanCodeAndItsNumber() {
        when(catalog.findPlanByCode("LEARNER_PLUS")).thenReturn(Optional.of(
                SubscriptionPlan.builder().id("p1").code("LEARNER_PLUS").build()));
        when(catalog.findVersion("p1", 2)).thenReturn(Optional.of(SubscriptionPlanVersion.builder().id("v1").planId("p1").versionNumber(2).build()));
        when(catalog.findPlanByCode("OTHER")).thenReturn(Optional.empty());

        assertThat(service.findVersion("LEARNER_PLUS", 2)).contains(new PlanVersionRef("v1", "p1", "LEARNER_PLUS", 2));
        assertThat(service.planExists("LEARNER_PLUS")).isTrue();
        assertThat(service.planExists("OTHER")).isFalse();
        assertThat(service.findVersion("OTHER", 1)).isEmpty();
    }

    @Test
    void grantsComeBackAsPlainRecordsInFeatureKeyOrder() {
        when(catalog.findEntitlements("v1")).thenReturn(List.of(PlanEntitlement.builder().planVersionId("v1").featureKey("a.quota")
                .grantType("QUOTA").quotaLimit(200L).quotaUnlimited(false).quotaUnit("REQUEST").resetPolicy("CALENDAR_MONTH").build()));

        assertThat(service.grantsOf("v1")).containsExactly(
                new PlanFeatureGrant("a.quota", "QUOTA", null, 200L, false, "REQUEST", "CALENDAR_MONTH"));
    }

    private static PlanOffer offer(String id, String priceId, LocalDateTime start, LocalDateTime end) {
        return PlanOffer.builder().id(id).planVersionId("v1").planId("p1").priceId(priceId).marketCode("VN").status("ACTIVE")
                .salesStart(start).salesEnd(end).build();
    }
}
