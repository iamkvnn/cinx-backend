package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.application.query.AccountAuthorizationQueries;
import com.cinx.auth.identity.application.query.AccountRoles;
import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AccountAuthorizationQueriesAdapter implements AccountAuthorizationQueries {
    private final IdentityAccountJpaRepository accounts;
    private final IdentityAccountRoleJpaRepository roles;

    @Override
    public List<AccountRoles> findActiveAccountRoles(LocalDateTime now) {
        Map<String, Set<AuthorizationRole>> effective = roles.findAllEffective(now).stream()
                .collect(Collectors.groupingBy(IdentityAccountRoleJpaEntity::getUserId,
                        Collectors.mapping(IdentityAccountRoleJpaEntity::getRoleCode, Collectors.toCollection(() -> EnumSet.noneOf(AuthorizationRole.class)))));
        return accounts.findAllByStatus(IdentityStatus.ACTIVE).stream()
                .map(account -> new AccountRoles(account.getUserId(),
                        effective.getOrDefault(account.getUserId(), EnumSet.noneOf(AuthorizationRole.class))))
                .toList();
    }
}
