package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.application.query.PublishedPlanView;
import com.cinx.commerce.catalog.domain.CommerceFeatureRegistry;
import com.cinx.commerce.catalog.domain.PlanCatalogRepository;
import com.cinx.commerce.catalog.domain.PlanEntitlement;
import com.cinx.commerce.catalog.domain.PlanOffer;
import com.cinx.commerce.catalog.domain.PlanPrice;
import com.cinx.commerce.catalog.domain.PlanProductLine;
import com.cinx.commerce.catalog.domain.PlanVersionTranslation;
import com.cinx.commerce.catalog.domain.SubscriptionPlan;
import com.cinx.commerce.catalog.domain.SubscriptionPlanVersion;
import com.cinx.commerce.subscription.application.BillingAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** What the public plan catalog tells a client: the copy in its language, the price of each offer, the features worth listing. */
@ExtendWith(MockitoExtension.class)
class PlanCatalogServiceTest {
    private static final String VERSION_ID = "version-1";
    private static final String PLAN_ID = "plan-1";

    @Mock private PlanCatalogRepository catalog;
    @Mock private BillingAvailability billing;
    private PlanCatalogService service;

    @BeforeEach
    void publishOnePlan() {
        service = new PlanCatalogService(catalog, billing, Clock.systemUTC());
        lenient().when(catalog.findPublishedVersions())
                .thenReturn(List.of(SubscriptionPlanVersion.builder().id(VERSION_ID).planId(PLAN_ID).versionNumber(1)
                        .defaultLocale("vi-VN").publicationStatus("PUBLISHED").build()));
        lenient().when(catalog.findPlanById(PLAN_ID)).thenReturn(Optional.of(SubscriptionPlan.builder().id(PLAN_ID).code("LEARNER_PLUS")
                .productLineId("line-1").build()));
        lenient().when(catalog.findProductLineById("line-1")).thenReturn(Optional.of(PlanProductLine.builder().id("line-1").code("LEARNING").build()));
        lenient().when(catalog.findEntitlements(VERSION_ID)).thenReturn(List.of());
        lenient().when(catalog.findOffers(VERSION_ID, "ACTIVE")).thenReturn(List.of());
    }

    @Test
    void usesTheRequestedLocaleWhenItHasACopy() {
        when(catalog.findTranslation(VERSION_ID, "en-US")).thenReturn(Optional.of(copy("en-US", "Learner Plus", "Standard catalog")));

        PublishedPlanView plan = only(service.publishedPlans("VN", "en-US"));

        assertThat(plan.name()).isEqualTo("Learner Plus");
        assertThat(plan.description()).isEqualTo("Standard catalog");
    }

    @Test
    void fallsBackToTheVersionsDefaultLocaleWhenTheRequestedOneHasNoCopy() {
        when(catalog.findTranslation(VERSION_ID, "fr-FR")).thenReturn(Optional.empty());
        when(catalog.findTranslation(VERSION_ID, "vi-VN")).thenReturn(Optional.of(copy("vi-VN", "Học viên Plus", "Catalog tiêu chuẩn")));

        assertThat(only(service.publishedPlans("VN", "fr-FR")).name()).isEqualTo("Học viên Plus");
    }

    @Test
    void usesTheDefaultLocaleWhenNoneIsRequested() {
        when(catalog.findTranslation(VERSION_ID, "vi-VN")).thenReturn(Optional.of(copy("vi-VN", "Học viên Plus", null)));

        PublishedPlanView plan = only(service.publishedPlans("VN", null));

        assertThat(plan.name()).isEqualTo("Học viên Plus");
        assertThat(plan.description()).isNull();
    }

    @Test
    void showsThePlanCodeAsTheNameWhenThereIsNoCopyAtAll() {
        when(catalog.findTranslation(VERSION_ID, "vi-VN")).thenReturn(Optional.empty());

        PublishedPlanView plan = only(service.publishedPlans("VN", null));

        assertThat(plan.name()).isEqualTo("LEARNER_PLUS");
        assertThat(plan.badge()).isNull();
    }

    @Test
    void listsOnlyFeaturesTheRegistryMarksSaleableAndActive() {
        noCopy();
        when(catalog.findEntitlements(VERSION_ID)).thenReturn(List.of(
                bool("a.sold", true), bool("b.not-saleable", true), bool("c.inactive", true), bool("d.unknown", true)));
        when(catalog.findFeatures(any())).thenReturn(List.of(
                feature("a.sold", "BOOLEAN", true, true),
                feature("b.not-saleable", "BOOLEAN", false, true),
                feature("c.inactive", "BOOLEAN", true, false)));

        List<PublishedPlanView.Feature> features = only(service.publishedPlans("VN", null)).features();

        // d.unknown has no registry row, b is not saleable, c is not active: only a is public
        assertThat(features).extracting(PublishedPlanView.Feature::key).containsExactly("a.sold");
    }

    @Test
    void aBooleanFeatureIsEnabledOnlyWhenItsValueIsTrue() {
        noCopy();
        when(catalog.findEntitlements(VERSION_ID)).thenReturn(List.of(bool("a.on", true), bool("b.off", false)));
        when(catalog.findFeatures(any())).thenReturn(List.of(feature("a.on", "BOOLEAN", true, true), feature("b.off", "BOOLEAN", true, true)));

        List<PublishedPlanView.Feature> features = only(service.publishedPlans("VN", null)).features();

        assertThat(features).extracting(PublishedPlanView.Feature::key, PublishedPlanView.Feature::enabled).containsExactly(
                org.assertj.core.groups.Tuple.tuple("a.on", true), org.assertj.core.groups.Tuple.tuple("b.off", false));
    }

    @Test
    void aQuotaIsEnabledWhenItHasALimitAboveZeroOrIsUnlimited() {
        noCopy();
        when(catalog.findEntitlements(VERSION_ID)).thenReturn(List.of(
                quota("a.limited", 200L, false), quota("b.zero", 0L, false), quota("c.unlimited", null, true)));
        when(catalog.findFeatures(any())).thenReturn(List.of(
                feature("a.limited", "QUOTA", true, true), feature("b.zero", "QUOTA", true, true), feature("c.unlimited", "QUOTA", true, true)));

        List<PublishedPlanView.Feature> features = only(service.publishedPlans("VN", null)).features();

        assertThat(features).extracting(PublishedPlanView.Feature::key, PublishedPlanView.Feature::enabled, PublishedPlanView.Feature::quotaLimit, PublishedPlanView.Feature::quotaUnlimited)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("a.limited", true, 200L, false),
                        org.assertj.core.groups.Tuple.tuple("b.zero", false, 0L, false),
                        org.assertj.core.groups.Tuple.tuple("c.unlimited", true, null, true));
    }

    @Test
    void anOfferCarriesThePriceOfItsPlanAndAFreeOfferCarriesNone() {
        noCopy();
        when(catalog.findOffers(VERSION_ID, "ACTIVE")).thenReturn(List.of(
                PlanOffer.builder().id("offer-month").planVersionId(VERSION_ID).planId(PLAN_ID).priceId("price-month").marketCode("VN").status("ACTIVE").build(),
                PlanOffer.builder().id("offer-free").planVersionId(VERSION_ID).planId(PLAN_ID).priceId(null).marketCode("VN").status("ACTIVE").build()));
        when(catalog.findPrice("price-month", PLAN_ID)).thenReturn(Optional.of(PlanPrice.builder().id("price-month").planId(PLAN_ID)
                .amount(new BigDecimal("99000.00")).currency("VND").billingInterval("MONTH").intervalCount(1).build()));

        List<PublishedPlanView.Offer> result = only(service.publishedPlans("VN", null)).offers();

        PublishedPlanView.Offer paid = result.get(0);
        assertThat(paid.offerId()).isEqualTo("offer-month");
        assertThat(paid.amount()).isEqualByComparingTo("99000");
        assertThat(paid.currency()).isEqualTo("VND");
        assertThat(paid.billingInterval()).isEqualTo("MONTH");
        assertThat(paid.intervalCount()).isEqualTo(1);
        PublishedPlanView.Offer free = result.get(1);
        assertThat(free.amount()).isNull();
        assertThat(free.currency()).isNull();
        assertThat(free.billingInterval()).isNull();
        assertThat(free.intervalCount()).isNull();
    }

    @Test
    void anOfferOfAnotherMarketIsNotListed() {
        noCopy();
        when(catalog.findOffers(VERSION_ID, "ACTIVE")).thenReturn(List.of(
                PlanOffer.builder().id("offer-vn").planVersionId(VERSION_ID).planId(PLAN_ID).marketCode("VN").status("ACTIVE").build(),
                PlanOffer.builder().id("offer-us").planVersionId(VERSION_ID).planId(PLAN_ID).marketCode("US").status("ACTIVE").build()));

        assertThat(only(service.publishedPlans("vn", null)).offers()).extracting(PublishedPlanView.Offer::offerId).containsExactly("offer-vn");
    }

    @Test
    void isPurchasableExactlyWhenBillingIsAvailable() {
        noCopy();
        when(billing.available()).thenReturn(true, false);

        assertThat(only(service.publishedPlans("VN", null)).purchasable()).as("billing available").isTrue();
        assertThat(only(service.publishedPlans("VN", null)).purchasable()).as("billing not available").isFalse();
    }

    private void noCopy() {
        when(catalog.findTranslation(VERSION_ID, "vi-VN")).thenReturn(Optional.empty());
    }

    private static PublishedPlanView only(List<PublishedPlanView> list) {
        assertThat(list).hasSize(1);
        return list.get(0);
    }

    private static PlanVersionTranslation copy(String locale, String name, String description) {
        PlanVersionTranslation translation = new PlanVersionTranslation();
        translation.setPlanVersionId(VERSION_ID);
        translation.setLocale(locale);
        translation.setDisplayName(name);
        translation.setDescription(description);
        return translation;
    }

    private static CommerceFeatureRegistry feature(String key, String valueType, boolean saleable, boolean active) {
        CommerceFeatureRegistry feature = new CommerceFeatureRegistry();
        feature.setFeatureKey(key);
        feature.setValueType(valueType);
        feature.setSaleable(saleable);
        feature.setActive(active);
        return feature;
    }

    private static PlanEntitlement bool(String key, boolean value) {
        return PlanEntitlement.builder().planVersionId(VERSION_ID).featureKey(key).grantType("BOOLEAN").booleanValue(value).build();
    }

    private static PlanEntitlement quota(String key, Long limit, boolean unlimited) {
        return PlanEntitlement.builder().planVersionId(VERSION_ID).featureKey(key).grantType("QUOTA").quotaLimit(limit)
                .quotaUnlimited(unlimited).quotaUnit("REQUEST").resetPolicy("CALENDAR_MONTH").build();
    }
}
