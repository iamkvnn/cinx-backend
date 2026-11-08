package com.cinx.commerce.voucher.infrastructure.persistence;

import com.cinx.commerce.voucher.domain.Voucher;
import com.cinx.commerce.voucher.domain.VoucherTargetRule;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Maps a voucher and its target rules in both directions; written by hand because the rules are a child collection. */
@Component
class VoucherPersistenceMapper {

    Voucher toDomain(VoucherJpaEntity entity) {
        Set<VoucherTargetRule> rules = new HashSet<>();
        entity.getTargetRules().forEach(rule -> rules.add(VoucherTargetRule.builder().id(rule.getId())
                .targetType(rule.getTargetType()).matchType(rule.getMatchType()).targetId(rule.getTargetId()).build()));
        return Voucher.builder().id(entity.getId()).code(entity.getCode()).discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue()).maxDiscountAmount(entity.getMaxDiscountAmount())
                .minimumEligibleAmount(entity.getMinimumEligibleAmount()).minimumEligibleItems(entity.getMinimumEligibleItems())
                .totalUsageLimit(entity.getTotalUsageLimit()).usedCount(entity.getUsedCount()).perUserLimit(entity.getPerUserLimit())
                .firstPurchaseOnly(entity.isFirstPurchaseOnly()).validFrom(entity.getValidFrom()).validTo(entity.getValidTo())
                .active(entity.isActive()).targetRules(rules).build();
    }

    VoucherJpaEntity toEntity(Voucher voucher) {
        VoucherJpaEntity entity = new VoucherJpaEntity();
        entity.setId(voucher.getId());
        update(entity, voucher);
        return entity;
    }

    /**
     * Writes the domain's changes onto the stored row. The target rules are matched by id: rules that are gone are
     * removed and new ones added, and the rows that did not change are left as they are.
     */
    void update(VoucherJpaEntity entity, Voucher voucher) {
        entity.setCode(voucher.getCode());
        entity.setDiscountType(voucher.getDiscountType());
        entity.setDiscountValue(voucher.getDiscountValue());
        entity.setMaxDiscountAmount(voucher.getMaxDiscountAmount());
        entity.setMinimumEligibleAmount(voucher.getMinimumEligibleAmount());
        entity.setMinimumEligibleItems(voucher.getMinimumEligibleItems());
        entity.setTotalUsageLimit(voucher.getTotalUsageLimit());
        entity.setUsedCount(voucher.getUsedCount());
        entity.setPerUserLimit(voucher.getPerUserLimit());
        entity.setFirstPurchaseOnly(voucher.isFirstPurchaseOnly());
        entity.setValidFrom(voucher.getValidFrom());
        entity.setValidTo(voucher.getValidTo());
        entity.setActive(voucher.isActive());
        Set<String> wanted = voucher.getTargetRules().stream().map(VoucherTargetRule::getId).collect(Collectors.toSet());
        entity.getTargetRules().removeIf(row -> !wanted.contains(row.getId()));
        Set<String> kept = entity.getTargetRules().stream().map(VoucherTargetRuleJpaEntity::getId).collect(Collectors.toSet());
        voucher.getTargetRules().stream().filter(rule -> !kept.contains(rule.getId())).forEach(rule -> {
            VoucherTargetRuleJpaEntity row = new VoucherTargetRuleJpaEntity();
            row.setId(rule.getId());
            row.setVoucher(entity);
            row.setTargetType(rule.getTargetType());
            row.setMatchType(rule.getMatchType());
            row.setTargetId(rule.getTargetId());
            entity.getTargetRules().add(row);
        });
    }
}
