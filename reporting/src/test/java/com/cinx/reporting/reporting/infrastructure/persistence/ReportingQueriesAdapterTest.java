package com.cinx.reporting.reporting.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingQueriesAdapterTest {
    @Mock
    private AccountProjectionRepository accounts;
    @Mock
    private OrderProjectionRepository orders;
    @Mock
    private EnrollmentProjectionRepository enrollments;
    private ReportingQueriesAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ReportingQueriesAdapter(accounts, orders, enrollments);
    }

    @Test
    void activeAccountsAreThoseWithStatusActive() {
        // only ACTIVE is stubbed: asking for any other status would answer 0 and fail the assertion
        when(accounts.countByStatus("ACTIVE")).thenReturn(7L);

        assertThat(adapter.countActiveAccounts()).isEqualTo(7);
    }

    @Test
    void paidOrdersAreThoseWithStatusPaid() {
        // only PAID is stubbed: asking for any other status would answer 0 and fail the assertion
        when(orders.countByStatus("PAID")).thenReturn(3L);

        assertThat(adapter.countPaidOrders()).isEqualTo(3);
    }

    @Test
    void revenueComesFromTheProjectionAndIsZeroWhenThereIsNone() {
        when(orders.paidRevenue()).thenReturn(new BigDecimal("199000.00"), (BigDecimal) null);

        assertThat(adapter.paidRevenue()).isEqualByComparingTo("199000.00");
        assertThat(adapter.paidRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void enrollmentsAreEveryProjectedRow() {
        when(enrollments.count()).thenReturn(12L);

        assertThat(adapter.countEnrollments()).isEqualTo(12);
    }
}
