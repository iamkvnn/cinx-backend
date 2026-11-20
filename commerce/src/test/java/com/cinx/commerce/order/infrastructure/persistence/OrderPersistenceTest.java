package com.cinx.commerce.order.infrastructure.persistence;

import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.order.domain.CommerceOrderItem;
import com.cinx.commerce.order.domain.OrderStatus;
import com.cinx.commerce.shared.domain.DiscountType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock CommerceOrderJpaRepository jpa;
    OrderPersistenceMapper mapper = new OrderPersistenceMapper();
    OrderRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrderRepositoryAdapter(jpa, mapper);
    }

    @Test
    void aNewOrderBecomesAnEntityWithOneRowPerLineEachPointingBackAtIt() {
        CommerceOrder order = order();

        CommerceOrderJpaEntity entity = mapper.toEntity(order);

        assertThat(entity.getId()).isEqualTo("o1");
        assertThat(entity.getUserId()).isEqualTo("user-1");
        assertThat(entity.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(entity.getVoucherDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(entity.getItems()).hasSize(1);
        CommerceOrderItemJpaEntity line = entity.getItems().get(0);
        assertThat(line.getOrder()).isSameAs(entity);
        assertThat(line.getCourseId()).isEqualTo("c1");
        assertThat(line.getAllocatedAmount()).isEqualByComparingTo("70000");
        assertThat(line.getPlatformFeePercent()).isEqualByComparingTo("30");
    }

    @Test
    void aStoredOrderComesBackAsTheDomainModelWithItsLines() {
        CommerceOrder back = mapper.toDomain(mapper.toEntity(order()));

        assertThat(back.getId()).isEqualTo("o1");
        assertThat(back.getTotalAmount()).isEqualByComparingTo("70000");
        assertThat(back.getVoucherCode()).isEqualTo("SAVE10");
        assertThat(back.getItems()).extracting(CommerceOrderItem::getId, CommerceOrderItem::getTitleSnapshot, CommerceOrderItem::getInstructorId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("i1", "Title", "inst"));
    }

    @Test
    void paymentOrCancellationWritesTheHeaderOntoTheStoredRowAndLeavesItsLinesAlone() {
        CommerceOrderJpaEntity stored = mapper.toEntity(order());
        CommerceOrderItemJpaEntity line = stored.getItems().get(0);
        when(jpa.findById("o1")).thenReturn(Optional.of(stored));
        when(jpa.save(stored)).thenReturn(stored);
        CommerceOrder changed = mapper.toDomain(stored);
        changed.markPaid(NOW);

        adapter.save(changed);

        assertThat(stored.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(stored.getPaidAt()).isEqualTo(NOW);
        assertThat(stored.getItems()).containsExactly(line);
        verify(jpa, never()).saveAndFlush(any());
    }

    @Test
    void aNewOrderIsWrittenOutAtOnceSoRowsThatPointAtItCanFollow() {
        when(jpa.findById("o1")).thenReturn(Optional.empty());
        when(jpa.saveAndFlush(any(CommerceOrderJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommerceOrder saved = adapter.save(order());

        assertThat(saved.getId()).isEqualTo("o1");
        verify(jpa).saveAndFlush(any(CommerceOrderJpaEntity.class));
    }

    private static CommerceOrder order() {
        return CommerceOrder.builder().id("o1").userId("user-1").originalAmount(new BigDecimal("100000.00"))
                .discountAmount(new BigDecimal("30000.00")).totalAmount(new BigDecimal("70000.00")).voucherCode("SAVE10")
                .voucherDiscountType(DiscountType.FIXED).voucherDiscountValue(new BigDecimal("30000.00")).idempotencyKey("key-1")
                .requestFingerprint("fp").status(OrderStatus.PENDING).createdAt(NOW)
                .items(List.of(CommerceOrderItem.builder().id("i1").courseId("c1").titleSnapshot("Title")
                        .priceSnapshot(new BigDecimal("100000.00")).instructorId("inst").allocatedAmount(new BigDecimal("70000.00"))
                        .platformFeePercent(new BigDecimal("30")).build())).build();
    }
}
