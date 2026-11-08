package com.cinx.commerce.voucher.application;

import com.cinx.commerce.cart.application.CartContents;
import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.order.application.PurchaseHistory;
import com.cinx.commerce.shared.domain.DiscountType;
import com.cinx.commerce.voucher.application.command.CreateVoucherCommand;
import com.cinx.commerce.voucher.application.command.UpdateVoucherCommand;
import com.cinx.commerce.voucher.application.command.VoucherTargetRuleCommand;
import com.cinx.commerce.voucher.application.query.VoucherPreview;
import com.cinx.commerce.voucher.application.query.VoucherView;
import com.cinx.commerce.voucher.domain.Voucher;
import com.cinx.commerce.voucher.domain.VoucherCalculation;
import com.cinx.commerce.voucher.domain.VoucherRedemption;
import com.cinx.commerce.voucher.domain.VoucherRedemptionRepository;
import com.cinx.commerce.voucher.domain.VoucherRepository;
import com.cinx.commerce.voucher.domain.VoucherTargetRule;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService implements IVoucherService, VoucherCheckout {
    private final VoucherRepository repository;
    private final VoucherRedemptionRepository redemptions;
    private final PurchaseHistory purchases;
    private final CartContents cart;
    private final SellableCourses courses;
    private final VoucherPolicy policy;

    @Override
    @Transactional
    public VoucherView create(CreateVoucherCommand command) {
        validate(command.discountType(), command.discountValue(), command.maxDiscountAmount(), command.totalUsageLimit(),
                command.perUserLimit(), command.validFrom(), command.validTo());
        String code = normalize(command.code());
        if (repository.existsByCode(code)) throw new AlreadyExistException("Voucher code already exists");
        Voucher voucher = Voucher.builder().id(UUID.randomUUID().toString()).code(code).discountType(command.discountType())
                .discountValue(command.discountValue()).maxDiscountAmount(command.maxDiscountAmount())
                .minimumEligibleAmount(command.minimumEligibleAmount()).minimumEligibleItems(command.minimumEligibleItems())
                .totalUsageLimit(command.totalUsageLimit()).usedCount(0L).perUserLimit(command.perUserLimit())
                .firstPurchaseOnly(command.firstPurchaseOnly()).validFrom(command.validFrom()).validTo(command.validTo())
                .active(true).build();
        voucher.replaceTargetRules(rules(command.targetRules()));
        return VoucherView.of(repository.save(voucher));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<VoucherView> findAll(PageQuery page) {
        return repository.findAll(page).map(VoucherView::of);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherView findByCode(String code) {
        return VoucherView.of(detailed(code));
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherPreview preview(String userId, String code) {
        Voucher voucher = detailed(code);
        if (redemptions.countByVoucherIdAndUserId(voucher.getId(), userId) >= voucher.getPerUserLimit())
            throw new BadRequestException(ErrorCode.VOUCHER_INVALID, "Voucher per-user limit has been reached");
        if (voucher.isFirstPurchaseOnly() && (redemptions.existsFirstPurchaseClaim(userId) || purchases.hasPaidOrder(userId)))
            throw new BadRequestException(ErrorCode.VOUCHER_INVALID, "Voucher is only valid for a first purchase");
        List<SellableCourseView> cartCourses = cartCourses(userId);
        BigDecimal original = cartCourses.stream().map(SellableCourseView::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        VoucherCalculation result = policy.calculate(voucher, cartCourses);
        return new VoucherPreview(voucher.getCode(), result.eligibleSubtotal(), result.discountAmount(),
                original.subtract(result.discountAmount()));
    }

    @Override
    @Transactional
    public VoucherView update(String code, UpdateVoucherCommand command) {
        Voucher voucher = detailed(code);
        if (redemptions.existsByVoucherId(voucher.getId()))
            throw new BadRequestException("A redeemed voucher can only be deactivated");
        validate(command.discountType(), command.discountValue(), command.maxDiscountAmount(), command.totalUsageLimit(),
                command.perUserLimit(), command.validFrom(), command.validTo());
        voucher.setDiscountType(command.discountType());
        voucher.setDiscountValue(command.discountValue());
        voucher.setMaxDiscountAmount(command.maxDiscountAmount());
        voucher.setMinimumEligibleAmount(command.minimumEligibleAmount());
        voucher.setMinimumEligibleItems(command.minimumEligibleItems());
        voucher.setTotalUsageLimit(command.totalUsageLimit());
        voucher.setPerUserLimit(command.perUserLimit());
        voucher.setFirstPurchaseOnly(command.firstPurchaseOnly());
        voucher.setValidFrom(command.validFrom());
        voucher.setValidTo(command.validTo());
        voucher.replaceTargetRules(rules(command.targetRules()));
        return VoucherView.of(repository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherView setActive(String code, boolean active) {
        Voucher voucher = detailed(code);
        voucher.setActive(active);
        return VoucherView.of(repository.save(voucher));
    }

    @Override
    @Transactional
    public AppliedVoucher apply(String userId, String code, List<SellableCourseView> cartCourses) {
        Voucher voucher = repository.lockByCode(code).orElseThrow(() -> new NotFoundException("Voucher not found"));
        if (redemptions.countByVoucherIdAndUserId(voucher.getId(), userId) >= voucher.getPerUserLimit())
            throw new BadRequestException(ErrorCode.VOUCHER_INVALID, "Voucher per-user limit has been reached");
        if (voucher.isFirstPurchaseOnly()) {
            if (purchases.hasPaidOrder(userId) || redemptions.existsFirstPurchaseClaim(userId))
                throw new BadRequestException(ErrorCode.VOUCHER_INVALID, "Voucher is only valid for a first purchase");
        }
        VoucherCalculation calculation = policy.calculate(voucher, cartCourses);
        Set<String> discounted = cartCourses.stream().filter(course -> policy.appliesTo(voucher, course))
                .map(SellableCourseView::courseId).collect(Collectors.toSet());
        return new AppliedVoucher(voucher.getId(), voucher.getCode(), voucher.getDiscountType(), voucher.getDiscountValue(),
                calculation.eligibleSubtotal(), calculation.discountAmount(), discounted, voucher.isFirstPurchaseOnly());
    }

    @Override
    @Transactional
    public void consume(AppliedVoucher applied, String userId, String orderId, LocalDateTime now) {
        redemptions.save(VoucherRedemption.builder().id(UUID.randomUUID().toString()).voucherId(applied.voucherId())
                .userId(userId).orderId(orderId).eligibleSubtotal(applied.eligibleSubtotal())
                .discountAmount(applied.discountAmount()).consumedAt(now).firstPurchaseClaim(applied.firstPurchaseOnly()).build());
        Voucher voucher = repository.findById(applied.voucherId()).orElseThrow(() -> new NotFoundException("Voucher not found"));
        voucher.recordUse();
        repository.save(voucher);
    }

    private Voucher detailed(String code) {
        return repository.findByCode(normalize(code)).orElseThrow(() -> new NotFoundException("Voucher not found"));
    }

    private List<SellableCourseView> cartCourses(String userId) {
        List<String> courseIds = cart.courseIdsOf(userId);
        if (courseIds.isEmpty()) throw new BadRequestException("Cart is empty");
        return courseIds.stream().map(id -> courses.findSellable(id)
                .orElseThrow(() -> new BadRequestException(ErrorCode.COURSE_UNAVAILABLE_FOR_PURCHASE, "Cart contains a course that is not sellable"))).toList();
    }

    private Set<VoucherTargetRule> rules(Set<VoucherTargetRuleCommand> commands) {
        Set<VoucherTargetRule> rules = new HashSet<>();
        if (commands == null) return rules;
        commands.forEach(rule -> rules.add(VoucherTargetRule.builder().id(UUID.randomUUID().toString())
                .targetType(rule.targetType()).matchType(rule.matchType()).targetId(rule.targetId()).build()));
        return rules;
    }

    private void validate(DiscountType type, BigDecimal value, BigDecimal max, long total, long perUser,
                          LocalDateTime from, LocalDateTime to) {
        if (!from.isBefore(to)) throw new BadRequestException("Voucher date range is invalid");
        if (type == DiscountType.PERCENT && (value.compareTo(BigDecimal.valueOf(100)) > 0 || max == null))
            throw new BadRequestException("Percent voucher requires value 1..100 and maxDiscountAmount");
        if (type == DiscountType.FIXED && max != null)
            throw new BadRequestException("Fixed voucher must not define maxDiscountAmount");
        if (perUser > total) throw new BadRequestException("Per-user limit cannot exceed total usage limit");
    }

    private String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
