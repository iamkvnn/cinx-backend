package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.BillingWebhookInbox;
import com.cinx.commerce.subscription.domain.CommerceSubscription;
import com.cinx.commerce.subscription.domain.CurrentSubscriptionPointer;
import com.cinx.commerce.subscription.domain.SubscriptionCommand;
import com.cinx.commerce.subscription.domain.SubscriptionSubject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPersistenceTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    // ---- subjects

    @Test
    void anExistingSubjectIsReturnedAsIsAndNothingIsInserted(@Mock SubscriptionSubjectJpaRepository jpa) {
        SubscriptionSubjectRepositoryAdapter adapter = new SubscriptionSubjectRepositoryAdapter(jpa, new SubscriptionSubjectPersistenceMapperImpl());
        SubscriptionSubjectJpaEntity stored = new SubscriptionSubjectJpaEntity();
        stored.setId("s1");
        stored.setSubjectType("USER");
        stored.setSubjectReference("user-1");
        when(jpa.findBySubjectTypeAndSubjectReference("USER", "user-1")).thenReturn(Optional.of(stored));

        assertThat(adapter.findOrCreate(candidate("new-id")).getId()).isEqualTo("s1");

        verify(jpa, never()).saveAndFlush(any());
    }

    @Test
    void whenTwoCallersRaceTheLoserReadsTheWinnersSubject(@Mock SubscriptionSubjectJpaRepository jpa) {
        SubscriptionSubjectRepositoryAdapter adapter = new SubscriptionSubjectRepositoryAdapter(jpa, new SubscriptionSubjectPersistenceMapperImpl());
        SubscriptionSubjectJpaEntity winner = new SubscriptionSubjectJpaEntity();
        winner.setId("winner");
        winner.setSubjectType("USER");
        winner.setSubjectReference("user-1");
        when(jpa.lockBySubjectTypeAndSubjectReference("USER", "user-1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(jpa.saveAndFlush(any(SubscriptionSubjectJpaEntity.class))).thenThrow(new DataIntegrityViolationException("uk_subscription_subject"));

        assertThat(adapter.lockOrCreate(candidate("loser")).getId()).isEqualTo("winner");
    }

    // ---- commands

    @Test
    void aCommandWithTheSameKeyAlreadyStoredIsNotInsertedAgain(@Mock SubscriptionCommandJpaRepository jpa) {
        SubscriptionCommandRepositoryAdapter adapter = new SubscriptionCommandRepositoryAdapter(jpa, new SubscriptionCommandPersistenceMapperImpl());
        when(jpa.saveAndFlush(any(SubscriptionCommandJpaEntity.class))).thenThrow(new DataIntegrityViolationException("uk_subscription_command_key"));

        assertThat(adapter.insertIfAbsent(command("cmd-1"))).isEmpty();
    }

    @Test
    void aNewCommandIsInsertedAndReturned(@Mock SubscriptionCommandJpaRepository jpa) {
        SubscriptionCommandRepositoryAdapter adapter = new SubscriptionCommandRepositoryAdapter(jpa, new SubscriptionCommandPersistenceMapperImpl());
        when(jpa.saveAndFlush(any(SubscriptionCommandJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adapter.insertIfAbsent(command("cmd-1"))).get().extracting(SubscriptionCommand::getCommandId).isEqualTo("cmd-1");
    }

    @Test
    void savingAStoredCommandChangesItsStatusOnTheStoredRow(@Mock SubscriptionCommandJpaRepository jpa) {
        SubscriptionCommandPersistenceMapperImpl mapper = new SubscriptionCommandPersistenceMapperImpl();
        SubscriptionCommandRepositoryAdapter adapter = new SubscriptionCommandRepositoryAdapter(jpa, mapper);
        SubscriptionCommandJpaEntity stored = mapper.toEntity(command("cmd-1"));
        when(jpa.findById("cmd-1")).thenReturn(Optional.of(stored));
        when(jpa.save(stored)).thenReturn(stored);
        SubscriptionCommand changed = command("cmd-1");
        changed.setStatus("COMPLETED");

        adapter.save(changed);

        assertThat(stored.getStatus()).isEqualTo("COMPLETED");
    }

    // ---- webhook inbox

    @Test
    void aWebhookEventRecordedBeforeIsReportedAsNotInserted(@Mock BillingWebhookInboxJpaRepository jpa) {
        BillingWebhookInboxRepositoryAdapter adapter = new BillingWebhookInboxRepositoryAdapter(jpa, new BillingWebhookInboxPersistenceMapperImpl());
        when(jpa.saveAndFlush(any(BillingWebhookInboxJpaEntity.class))).thenReturn(null).thenThrow(new DataIntegrityViolationException("pk"));

        assertThat(adapter.insertIfAbsent(inbox())).isTrue();
        assertThat(adapter.insertIfAbsent(inbox())).isFalse();
    }

    @Test
    void theInboxIsLookedUpByProviderAccountAndEventTogether(@Mock BillingWebhookInboxJpaRepository jpa) {
        BillingWebhookInboxRepositoryAdapter adapter = new BillingWebhookInboxRepositoryAdapter(jpa, new BillingWebhookInboxPersistenceMapperImpl());
        when(jpa.existsById(new BillingWebhookInboxJpaEntity.Key("STRIPE", "default", "evt-1"))).thenReturn(true);

        assertThat(adapter.exists("STRIPE", "default", "evt-1")).isTrue();
        assertThat(adapter.exists("STRIPE", "other", "evt-1")).isFalse();
    }

    // ---- lock versions stay the store's

    @Test
    void writingAPointerOrASubscriptionNeverOverwritesTheStoredLockVersion(
            @Mock CurrentSubscriptionPointerJpaRepository pointers, @Mock CommerceSubscriptionJpaRepository subscriptions) {
        CurrentSubscriptionPointerPersistenceMapperImpl pointerMapper = new CurrentSubscriptionPointerPersistenceMapperImpl();
        CurrentSubscriptionPointerJpaEntity storedPointer = new CurrentSubscriptionPointerJpaEntity();
        storedPointer.setSubjectId("s1");
        storedPointer.setSubscriptionSlotCode("BASE");
        storedPointer.setSubscriptionId("old");
        storedPointer.setOptimisticVersion(7L);
        when(pointers.findById(new CurrentSubscriptionPointerJpaEntity.Key("s1", "BASE"))).thenReturn(Optional.of(storedPointer));
        when(pointers.save(storedPointer)).thenReturn(storedPointer);

        new CurrentSubscriptionPointerRepositoryAdapter(pointers, pointerMapper).save(CurrentSubscriptionPointer.builder().subjectId("s1")
                .subscriptionSlotCode("BASE").subscriptionId("new").optimisticVersion(0L).build());

        assertThat(storedPointer.getSubscriptionId()).isEqualTo("new");
        assertThat(storedPointer.getOptimisticVersion()).isEqualTo(7L);

        CommerceSubscriptionPersistenceMapperImpl subscriptionMapper = new CommerceSubscriptionPersistenceMapperImpl();
        CommerceSubscriptionJpaEntity storedSubscription = new CommerceSubscriptionJpaEntity();
        storedSubscription.setId("sub-1");
        storedSubscription.setOptimisticVersion(4L);
        storedSubscription.setAccessStatus("ACTIVE");
        when(subscriptions.findById("sub-1")).thenReturn(Optional.of(storedSubscription));
        when(subscriptions.save(storedSubscription)).thenReturn(storedSubscription);

        new CommerceSubscriptionRepositoryAdapter(subscriptions, subscriptionMapper).save(CommerceSubscription.builder().id("sub-1")
                .accessStatus("INELIGIBLE").optimisticVersion(0L).build());

        assertThat(storedSubscription.getAccessStatus()).isEqualTo("INELIGIBLE");
        assertThat(storedSubscription.getOptimisticVersion()).isEqualTo(4L);
    }

    @Test
    void aNewSubscriptionIsWrittenOutAtOnce(@Mock CommerceSubscriptionJpaRepository subscriptions) {
        CommerceSubscriptionRepositoryAdapter adapter = new CommerceSubscriptionRepositoryAdapter(subscriptions, new CommerceSubscriptionPersistenceMapperImpl());
        when(subscriptions.findById("sub-1")).thenReturn(Optional.empty());
        when(subscriptions.saveAndFlush(any(CommerceSubscriptionJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adapter.save(CommerceSubscription.builder().id("sub-1").subjectId("s1").build());

        verify(subscriptions).saveAndFlush(any(CommerceSubscriptionJpaEntity.class));
    }

    private static SubscriptionSubject candidate(String id) {
        return SubscriptionSubject.builder().id(id).subjectType("USER").subjectReference("user-1").createdAt(NOW).build();
    }

    private static SubscriptionCommand command(String id) {
        return SubscriptionCommand.builder().commandId(id).subjectId("s1").subscriptionSlotCode("BASE").idempotencyKey("key-1")
                .requestFingerprint("fp").commandType("CANCEL").providerIdempotencyKey("cinx_" + id).status("PENDING").createdAt(NOW).build();
    }

    private static BillingWebhookInbox inbox() {
        return BillingWebhookInbox.builder().provider("STRIPE").providerAccountKey("default").providerEventId("evt-1")
                .payloadHash("h").receivedAt(NOW).processingStatus("RECEIVED").build();
    }
}
