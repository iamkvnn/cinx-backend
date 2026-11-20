package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.catalog.domain.SellableCourse;
import com.cinx.commerce.catalog.domain.SellableCourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellableCourseServiceTest {
    @Mock SellableCourseRepository repository;
    @InjectMocks SellableCourseService service;

    @Test
    void aCourseIsShownWithWhatTheOthersNeedToKnow() {
        when(repository.findById("c1")).thenReturn(Optional.of(course(true)));

        assertThat(service.findById("c1")).contains(new SellableCourseView("c1", "Course", new BigDecimal("10.00"), "cat", "inst", true));
    }

    @Test
    void onlyACourseOnSaleIsSellable() {
        when(repository.findById("on")).thenReturn(Optional.of(course(true)));
        when(repository.findById("off")).thenReturn(Optional.of(course(false)));
        when(repository.findById("gone")).thenReturn(Optional.empty());

        assertThat(service.findSellable("on")).isPresent();
        assertThat(service.findSellable("off")).isEmpty();
        assertThat(service.findSellable("gone")).isEmpty();
        // a course that is off sale is still known, so a cart can show it as unavailable
        assertThat(service.findById("off")).isPresent();
    }

    private static SellableCourse course(boolean sellable) {
        return SellableCourse.builder().courseId("c1").title("Course").price(new BigDecimal("10.00")).categoryId("cat")
                .instructorId("inst").sellable(sellable).updatedAt(LocalDateTime.now()).build();
    }
}
