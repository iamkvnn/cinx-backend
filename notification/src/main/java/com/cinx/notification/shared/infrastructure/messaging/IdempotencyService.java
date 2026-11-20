package com.cinx.notification.shared.infrastructure.messaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final InboxMessageRepository inboxMessageRepository;

    @Transactional(readOnly = true)
    public boolean isProcessed(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            log.warn("Received message without ID; processing without idempotency guard");
            return false;
        }
        return inboxMessageRepository.findById(messageId)
                .map(message -> "SUCCESS".equals(message.getStatus()))
                .orElse(false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }
        try {
            inboxMessageRepository.saveAndFlush(InboxMessage.builder()
                    .messageId(messageId)
                    .status("SUCCESS")
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Message with ID {} was already marked processed", messageId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean checkAndSave(String messageId) {
        if (isProcessed(messageId)) {
            return false;
        }
        markSuccess(messageId);
        return true;
    }
}
