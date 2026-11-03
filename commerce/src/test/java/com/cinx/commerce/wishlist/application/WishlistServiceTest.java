package com.cinx.commerce.wishlist.application;

import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.shared.application.DomainEvents;
import com.cinx.commerce.wishlist.application.query.WishlistEntryView;
import com.cinx.commerce.wishlist.domain.WishlistItem;
import com.cinx.commerce.wishlist.domain.WishlistRepository;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {
    @Mock WishlistRepository repository;
    @Mock SellableCourses courses;
    @Mock DomainEvents events;
    WishlistService service;

    @BeforeEach
    void setUp() {
        service = new WishlistService(repository, courses, events);
    }

    @Test
    void addPublishesRecommendationCompatiblePayload() {
        when(courses.findSellable("course-1")).thenReturn(Optional.of(course()));
        when(repository.save(any(WishlistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WishlistEntryView entry = service.add("user-1", "course-1");

        ArgumentCaptor<Map<String, Object>> payload = payloadCaptor();
        verify(events).publish(any(), any(), payload.capture());
        assertEquals("user-1", payload.getValue().get("userId"));
        assertEquals("course-1", payload.getValue().get("courseId"));
        assertTrue((Boolean) payload.getValue().get("added"));
        assertThat(entry.title()).isEqualTo("Course");
        assertThat(entry.price()).isEqualByComparingTo("10.00");
    }

    @Test
    void addingACourseThatIsNotOnSaleIsRefusedAndPublishesNothing() {
        when(courses.findSellable("course-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add("user-1", "course-1")).isInstanceOf(BadRequestException.class);
        verify(repository, never()).save(any());
        verify(events, never()).publish(any(), any(), any());
    }

    @Test
    void addingTheSameCourseTwiceIsAConflict() {
        when(courses.findSellable("course-1")).thenReturn(Optional.of(course()));
        when(repository.existsByUserIdAndCourseId("user-1", "course-1")).thenReturn(true);

        assertThatThrownBy(() -> service.add("user-1", "course-1")).isInstanceOf(AlreadyExistException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void removePublishesRecommendationCompatiblePayload() {
        when(repository.deleteByUserIdAndCourseId("user-1", "course-1")).thenReturn(true);

        service.remove("user-1", "course-1");

        ArgumentCaptor<Map<String, Object>> payload = payloadCaptor();
        verify(events).publish(any(), any(), payload.capture());
        assertFalse((Boolean) payload.getValue().get("added"));
    }

    @Test
    void removingAnItemThatIsNotThereIsNotFoundAndPublishesNothing() {
        when(repository.deleteByUserIdAndCourseId("user-1", "course-1")).thenReturn(false);

        assertThatThrownBy(() -> service.remove("user-1", "course-1")).isInstanceOf(NotFoundException.class);
        verify(events, never()).publish(any(), any(), any());
    }

    @Test
    void listingShowsEachItemWithTheCatalogsTitleAndPrice() {
        LocalDateTime added = LocalDateTime.of(2026, 8, 1, 9, 0);
        when(repository.findAllByUserId("user-1")).thenReturn(List.of(
                WishlistItem.builder().id("w1").userId("user-1").courseId("course-1").addedAt(added).build()));
        when(courses.findById("course-1")).thenReturn(Optional.of(course()));

        assertThat(service.findAll("user-1")).containsExactly(new WishlistEntryView("course-1", "Course", new BigDecimal("10.00"), added));
    }

    @Test
    void listingFailsWhenACoursesCatalogEntryIsMissing() {
        when(repository.findAllByUserId("user-1")).thenReturn(List.of(
                WishlistItem.builder().id("w1").userId("user-1").courseId("gone").addedAt(LocalDateTime.now()).build()));
        when(courses.findById("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAll("user-1")).isInstanceOf(NotFoundException.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Map<String, Object>> payloadCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }

    private SellableCourseView course() {
        return new SellableCourseView("course-1", "Course", new BigDecimal("10.00"), null, null, true);
    }
}
