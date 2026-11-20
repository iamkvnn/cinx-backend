package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.IdentityAccount;
import com.cinx.auth.identity.domain.IdentityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityAccountRepositoryAdapterTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Mock IdentityAccountJpaRepository accounts;
    IdentityAccountRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityAccountRepositoryAdapter(accounts, Mappers.getMapper(IdentityAccountPersistenceMapper.class));
    }

    @Test
    void lockByIdTakesTheWriteLockQueryNotThePlainFinder() {
        when(accounts.lockByUserId("user-1")).thenReturn(Optional.of(row("user-1", 3L)));

        assertThat(adapter.lockById("user-1")).get().extracting(IdentityAccount::getVersion).isEqualTo(3L);

        verify(accounts).lockByUserId("user-1");
        verify(accounts, never()).findById(any());
    }

    @Test
    void anUnknownAccountIsAbsent() {
        when(accounts.findById("ghost")).thenReturn(Optional.empty());

        assertThat(adapter.findById("ghost")).isEmpty();
    }

    @Test
    void aNewAccountIsInserted() {
        when(accounts.findById("user-1")).thenReturn(Optional.empty());
        when(accounts.save(any(IdentityAccountJpaEntity.class))).thenAnswer(call -> call.getArgument(0));

        adapter.save(IdentityAccount.create("user-1", NOW));

        ArgumentCaptor<IdentityAccountJpaEntity> saved = ArgumentCaptor.forClass(IdentityAccountJpaEntity.class);
        verify(accounts).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo("user-1");
        assertThat(saved.getValue().getVersion()).isNull();
    }

    @Test
    void anExistingAccountIsUpdatedInPlaceSoItsLockVersionStaysTheStoredOne() {
        IdentityAccountJpaEntity stored = row("user-1", 5L);
        when(accounts.findById("user-1")).thenReturn(Optional.of(stored));
        when(accounts.save(stored)).thenReturn(stored);
        IdentityAccount account = IdentityAccount.builder().userId("user-1").version(1L).status(IdentityStatus.BANNED)
                .changedAt(NOW).createdAt(NOW).updatedAt(NOW).build();

        adapter.save(account);

        assertThat(stored.getStatus()).isEqualTo(IdentityStatus.BANNED);
        assertThat(stored.getChangedAt()).isEqualTo(NOW);
        // the account in hand carried version 1, but the row keeps what the database holds
        assertThat(stored.getVersion()).isEqualTo(5L);
    }

    private static IdentityAccountJpaEntity row(String userId, long version) {
        return IdentityAccountJpaEntity.builder().userId(userId).status(IdentityStatus.ACTIVE)
                .createdAt(NOW).updatedAt(NOW).version(version).build();
    }
}
