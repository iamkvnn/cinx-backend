package com.cinx.commerce.voucher.domain;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import java.util.Optional;

/** Port: where vouchers, with their target rules, are stored. */
public interface VoucherRepository {
    Optional<Voucher> findById(String id);

    Optional<Voucher> findByCode(String code);

    /** The voucher with a row lock held until the surrounding transaction ends. */
    Optional<Voucher> lockByCode(String code);

    boolean existsByCode(String code);

    Voucher save(Voucher voucher);

    /** Vouchers in storage order; the page is one-based. */
    PageResult<Voucher> findAll(PageQuery page);
}
