package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityAccountRepository;
import com.cinx.auth.identity.domain.RoleGrant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class IdentityAccountRepositoryAdapter implements IdentityAccountRepository {
    private final IdentityAccountJpaRepository accounts;
    private final IdentityAccountRoleJpaRepository roles;
    private final IdentityAccountPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityAccount> findById(String userId) {
        return accounts.findById(userId).map(this::load);
    }

    @Override
    @Transactional
    public Optional<IdentityAccount> lockById(String userId) {
        return accounts.lockByUserId(userId).map(this::load);
    }

    /**
     * Writes the account row and each of its grants. A grant that is not in the aggregate is left alone: grants end
     * (revoked, expired) but are never deleted.
     */
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
        Map<AuthorizationRole, IdentityAccountRoleJpaEntity> stored = roles.findAllByUserId(account.getUserId()).stream()
                .collect(Collectors.toMap(IdentityAccountRoleJpaEntity::getRoleCode, Function.identity()));
        for (RoleGrant grant : account.getGrants()) {
            IdentityAccountRoleJpaEntity row = stored.get(grant.getRoleCode());
            if (row == null) {
                roles.save(mapper.toEntity(grant, account.getUserId()));
            } else {
                mapper.update(row, grant);
                roles.save(row);
            }
        }
        return load(entity);
    }

    @Override
    @Transactional
    public List<IdentityAccount> lockAccountsWithDueGrants(LocalDateTime now) {
        Set<String> userIds = roles.lockDue(now).stream().map(IdentityAccountRoleJpaEntity::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<IdentityAccount> locked = new ArrayList<>();
        userIds.forEach(userId -> lockById(userId).ifPresent(locked::add));
        return locked;
    }

    private IdentityAccount load(IdentityAccountJpaEntity entity) {
        IdentityAccount account = mapper.toDomain(entity);
        roles.findAllByUserId(entity.getUserId()).forEach(row -> account.getGrants().add(mapper.toGrant(row)));
        return account;
    }
}
