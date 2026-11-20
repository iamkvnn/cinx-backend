package com.cinx.commerce.voucher.infrastructure.persistence;

import com.cinx.commerce.voucher.domain.Voucher;
import com.cinx.commerce.voucher.domain.VoucherRepository;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class VoucherRepositoryAdapter implements VoucherRepository {
    private final VoucherJpaRepository jpa;
    private final VoucherPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> findByCode(String code) {
        return jpa.findDetailedByCode(code).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Voucher> lockByCode(String code) {
        return jpa.findByCodeForUpdate(code).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    /** Inserts a new voucher, or writes the changes onto the stored row so the rows that did not change stay put. */
    @Override
    @Transactional
    public Voucher save(Voucher voucher) {
        VoucherJpaEntity stored = jpa.findById(voucher.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(voucher)));
        }
        mapper.update(stored, voucher);
        return mapper.toDomain(jpa.save(stored));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Voucher> findAll(PageQuery query) {
        Page<VoucherJpaEntity> page = jpa.findAll(PageRequest.of(query.page() - 1, query.size()));
        return new PageResult<>(page.getContent().stream().map(mapper::toDomain).toList(), query.page(), query.size(),
                page.getTotalElements());
    }
}
