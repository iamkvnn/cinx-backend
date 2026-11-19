package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityAccountRepository;
import com.cinx.auth.identity.domain.IdentityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class IdentityAccountRepositoryAdapter implements IdentityAccountRepository {
    private final IdentityAccountJpaRepository accounts;
    private final IdentityAccountPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityAccount> findById(String userId) {
        return accounts.findById(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<IdentityAccount> lockById(String userId) {
        return accounts.lockByUserId(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findExpiredBans(LocalDateTime now, int limit) {
        return accounts.findUserIdsWithBanEndedBy(IdentityStatus.BANNED, now, PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public IdentityAccount save(IdentityAccount account) {
        IdentityAccountJpaEntity entity = accounts.findById(account.getUserId()).orElse(null);
        if (entity == null) {
            entity = accounts.save(mapper.toEntity(account));
        } else {
            mapper.update(entity, account);
            entity = accounts.save(entity);
        }
        return mapper.toDomain(entity);
    }
}
