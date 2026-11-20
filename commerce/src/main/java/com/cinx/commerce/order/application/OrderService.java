package com.cinx.commerce.order.application;

import com.cinx.commerce.cart.application.CartContents;
import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.order.application.command.CheckoutCommand;
import com.cinx.commerce.order.application.query.OrderView;
import com.cinx.commerce.order.domain.CommerceOrder;
import com.cinx.commerce.order.domain.CommerceOrderItem;
import com.cinx.commerce.order.domain.OrderRepository;
import com.cinx.commerce.order.domain.OrderStatus;
import com.cinx.commerce.voucher.application.AppliedVoucher;
import com.cinx.commerce.voucher.application.VoucherCheckout;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {
    private final OrderRepository orderRepository;
    private final CartContents cart;
    private final SellableCourses courses;
    private final VoucherCheckout vouchers;
    private final OrderPaidPublisher paidEvents;
    private final Clock clock;

    /** Snapshotted on each order line; changing it never rewrites past lines. */
    @Value("${cinx.commerce.platform-fee-percent:30}")
    private BigDecimal platformFeePercent;

    @Override
    @Transactional
    public OrderView checkout(String userId, String idempotencyKey, CheckoutCommand command) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 255)
            throw new BadRequestException("A valid Idempotency-Key header is required");
        String voucherCode = command == null || command.voucherCode() == null || command.voucherCode().isBlank()
                ? null : command.voucherCode().trim().toUpperCase(Locale.ROOT);
        String fingerprint = fingerprint(voucherCode == null ? "NO_VOUCHER" : voucherCode);
        Optional<CommerceOrder> previous = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (previous.isPresent()) {
            if (!previous.get().getRequestFingerprint().equals(fingerprint))
                throw new BadRequestException("Idempotency-Key was already used with another request");
            return OrderView.of(previous.get());
        }
        List<String> cartCourseIds = cart.courseIdsOf(userId);
        if (cartCourseIds.isEmpty()) throw new BadRequestException("Cart is empty");
        LocalDateTime now = LocalDateTime.now(clock);
        Set<String> owned = new HashSet<>(orderRepository.findPaidCourseIds(userId));
        if (cartCourseIds.stream().anyMatch(owned::contains))
            throw new ConflictException(ErrorCode.COURSE_ALREADY_OWNED, "Cart contains a course you already own");
        CommerceOrder order = CommerceOrder.builder().id(UUID.randomUUID().toString()).userId(userId)
                .idempotencyKey(idempotencyKey).requestFingerprint(fingerprint).status(OrderStatus.PENDING).createdAt(now).build();
        List<SellableCourseView> cartCourses = cartCourseIds.stream().map(courseId -> courses.findSellable(courseId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.COURSE_UNAVAILABLE_FOR_PURCHASE, "Cart contains a course that is not sellable"))).toList();
        List<CommerceOrderItem> items = cartCourses.stream().map(course -> CommerceOrderItem.builder()
                .id(UUID.randomUUID().toString()).courseId(course.courseId())
                .titleSnapshot(course.title()).priceSnapshot(course.price())
                .instructorId(course.instructorId()).build()).toList();
        BigDecimal original = items.stream().map(CommerceOrderItem::getPriceSnapshot).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = BigDecimal.ZERO;
        AppliedVoucher applied = null;
        if (voucherCode != null) {
            applied = vouchers.apply(userId, voucherCode, cartCourses);
            discount = applied.discountAmount();
            order.snapshotVoucher(applied.code(), applied.discountType(), applied.discountValue());
        }
        Set<String> discounted = applied == null ? Set.of() : applied.discountedCourseIds();
        OrderLineShares.allocate(items, discounted, discount, platformFeePercent);
        order.setItems(new ArrayList<>(items));
        order.setOriginalAmount(original);
        order.setDiscountAmount(discount);
        order.setTotalAmount(original.subtract(discount));
        // nothing to pay (a voucher covers the whole cart): the order is paid now, with its paid event in this transaction
        boolean free = order.isFree();
        if (free) {
            order.markPaid(now);
        }
        orderRepository.save(order);
        if (applied != null) vouchers.consume(applied, userId, order.getId(), now);
        if (free) paidEvents.publish(order);
        cart.clear(userId);
        return OrderView.of(order);
    }

    @Override
    @Transactional
    public OrderView cancel(String userId, String orderId) {
        CommerceOrder order = orderRepository.lockById(orderId)
                .filter(found -> found.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.isPending())
            throw new BadRequestException("Only a pending order can be cancelled");
        // the voucher redemption stays: voucher use is consumed at order creation and never silently restored
        order.cancel(LocalDateTime.now(clock));
        return OrderView.of(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderView> findAll(String userId) {
        return orderRepository.findAllByUserId(userId).stream().map(OrderView::of).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView findById(String userId, String orderId) {
        return OrderView.of(orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView findByIdAny(String orderId) {
        return OrderView.of(orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found")));
    }

    private String fingerprint(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
