package com.cinx.commerce.cart.application;

import com.cinx.commerce.cart.application.query.CartItemView;
import com.cinx.commerce.cart.domain.CartItem;
import com.cinx.commerce.cart.domain.CartRepository;
import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.order.application.PurchaseHistory;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock CartRepository repository;
    @Mock SellableCourses courses;
    @Mock PurchaseHistory purchases;
    CartService service;

    @BeforeEach
    void setUp() {
        service = new CartService(repository, courses, purchases, Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void addingASellableCourseStoresItWithTheCurrentTime() {
        when(courses.findSellable("c1")).thenReturn(Optional.of(course("c1", true)));
        when(purchases.paidCourseIds("user-1")).thenReturn(Set.of());
        when(repository.existsByUserIdAndCourseId("user-1", "c1")).thenReturn(false);
        when(repository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItemView view = service.add("user-1", "c1");

        assertThat(view.courseId()).isEqualTo("c1");
        assertThat(view.title()).isEqualTo("Title c1");
        assertThat(view.addedAt()).isEqualTo(NOW);
        assertThat(view.sellable()).isTrue();
        assertThat(view.owned()).isFalse();
    }

    @Test
    void aCourseThatIsNotOnSaleCannotBeAdded() {
        when(courses.findSellable("c1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add("user-1", "c1")).isInstanceOf(BadRequestException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void aCourseTheLearnerAlreadyBoughtCannotBeAdded() {
        when(courses.findSellable("c1")).thenReturn(Optional.of(course("c1", true)));
        when(purchases.paidCourseIds("user-1")).thenReturn(Set.of("c1"));

        assertThatThrownBy(() -> service.add("user-1", "c1")).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void aCourseAlreadyInTheCartCannotBeAddedTwice() {
        when(courses.findSellable("c1")).thenReturn(Optional.of(course("c1", true)));
        when(purchases.paidCourseIds("user-1")).thenReturn(Set.of());
        when(repository.existsByUserIdAndCourseId("user-1", "c1")).thenReturn(true);

        assertThatThrownBy(() -> service.add("user-1", "c1")).isInstanceOf(AlreadyExistException.class);
    }

    @Test
    void theCartMarksOwnedCoursesAndCoursesWithoutAProjectionInsteadOfFailing() {
        LocalDateTime added = NOW.minusHours(1);
        when(repository.findAllByUserId("user-1")).thenReturn(List.of(
                CartItem.builder().id("i1").userId("user-1").courseId("c1").addedAt(added).build(),
                CartItem.builder().id("i2").userId("user-1").courseId("gone").addedAt(added).build(),
                CartItem.builder().id("i3").userId("user-1").courseId("off").addedAt(added).build()));
        when(purchases.paidCourseIds("user-1")).thenReturn(Set.of("c1"));
        when(courses.findById("c1")).thenReturn(Optional.of(course("c1", true)));
        when(courses.findById("gone")).thenReturn(Optional.empty());
        when(courses.findById("off")).thenReturn(Optional.of(course("off", false)));

        List<CartItemView> items = service.findAll("user-1");

        assertThat(items).extracting(CartItemView::courseId, CartItemView::sellable, CartItemView::owned)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("c1", true, true),
                        org.assertj.core.groups.Tuple.tuple("gone", false, false),
                        org.assertj.core.groups.Tuple.tuple("off", false, false));
        assertThat(items.get(1).title()).isNull();
        assertThat(items.get(1).price()).isNull();
    }

    @Test
    void removingAnItemThatIsNotThereIsNotFound() {
        when(repository.deleteByUserIdAndCourseId("user-1", "c1")).thenReturn(false);

        assertThatThrownBy(() -> service.remove("user-1", "c1")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void checkoutReadsAndClearsTheCartThroughTheContentsPort() {
        when(repository.findAllByUserId("user-1")).thenReturn(List.of(
                CartItem.builder().id("i1").courseId("c2").build(), CartItem.builder().id("i2").courseId("c1").build()));

        assertThat(service.courseIdsOf("user-1")).containsExactly("c2", "c1");
        service.clear("user-1");

        verify(repository).deleteAllByUserId("user-1");
    }

    private static SellableCourseView course(String id, boolean sellable) {
        return new SellableCourseView(id, "Title " + id, new BigDecimal("10.00"), null, null, sellable);
    }
}
