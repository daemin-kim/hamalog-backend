package com.Hamalog.service.auth;

import com.Hamalog.domain.events.member.MemberDeletedEvent;
import com.Hamalog.security.jwt.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event handler for MemberDeletedEvent
 * Handles post-transaction cleanup operations like Redis token blacklisting
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberDeletedEventHandler {
    
    private final TokenBlacklistService tokenBlacklistService;
    
    /**
     * Handles member deletion events after transaction completion
     * Uses @TransactionalEventListener to ensure execution after DB transaction commits
     * Uses @Async to avoid blocking the main thread
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleMemberDeleted(MemberDeletedEvent event) {
        try {
            log.info("Processing member deletion cleanup for loginId: {}, memberId: {}", 
                    event.getLoginId(), event.getMemberId());
            
            // Blacklist the user's token in Redis
            if (event.getToken() != null && !event.getToken().isBlank()) {
                tokenBlacklistService.blacklistToken(event.getToken());
                log.info("Successfully blacklisted token for deleted member: {}", event.getLoginId());
            }
            
            // Additional cleanup operations can be added here
            // e.g., file cleanup, cache invalidation, external service notifications
            
            log.info("Member deletion cleanup completed for loginId: {}", event.getLoginId());
            
        } catch (Exception e) {
            // Log error but don't throw - we don't want to rollback the DB transaction
            // The member is already deleted from DB, Redis cleanup failure is not critical
            log.error("Failed to complete member deletion cleanup for loginId: {}, memberId: {}. Error: {}", 
                     event.getLoginId(), event.getMemberId(), e.getMessage(), e);
            
            // Could potentially add retry logic or dead letter queue here
            // For now, we just log the error for monitoring purposes
        }
    }
}