package com.Hamalog.service.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StatePersistenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ConcurrentHashMap<String, Long> stateWithExpiry = new ConcurrentHashMap<>();

    private static final String STATE_PREFIX = "oauth2_state:";
    private static final long STATE_TTL_MINUTES = 10;

    public StatePersistenceService(
            @Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * OAuth2 인증을 위한 state 값 생성 및 저장
     */
    public String generateAndStoreState() {
        String state = UUID.randomUUID().toString();
        storeState(state);
        log.debug("[OAUTH2] Generated and stored state: {}", state.substring(0, 8) + "...");
        return state;
    }

    /**
     * State 값 저장 (Redis 또는 메모리)
     */
    private void storeState(String state) {
        try {
            if (redisTemplate != null) {
                String redisKey = STATE_PREFIX + state;
                redisTemplate.opsForValue().set(redisKey, "active",
                    STATE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("[OAUTH2] State stored in Redis with TTL: {} minutes", STATE_TTL_MINUTES);
            } else {
                // 메모리 저장소 사용 (개발 환경)
                long expiryTime = System.currentTimeMillis() + (STATE_TTL_MINUTES * 60 * 1000);
                stateWithExpiry.put(state, expiryTime);
                log.warn("[OAUTH2] Using memory storage for state (development only)");
            }
        } catch (Exception e) {
            log.error("[OAUTH2] Failed to store state in Redis, falling back to memory", e);
            long expiryTime = System.currentTimeMillis() + (STATE_TTL_MINUTES * 60 * 1000);
            stateWithExpiry.put(state, expiryTime);
        }
    }

    /**
     * State 값 검증 (조회 시 삭제 = 일회용)
     */
    public boolean validateAndConsumeState(String state) {
        if (state == null || state.trim().isEmpty()) {
            log.warn("[OAUTH2] State validation failed: null or empty");
            return false;
        }

        try {
            if (redisTemplate != null) {
                String redisKey = STATE_PREFIX + state;
                Boolean exists = redisTemplate.hasKey(redisKey);

                if (Boolean.TRUE.equals(exists)) {
                    redisTemplate.delete(redisKey);  // 일회용 처리
                    log.debug("[OAUTH2] State validated and consumed from Redis");
                    return true;
                }
            }

            // 메모리 저장소 확인
            Long expiryTime = stateWithExpiry.get(state);
            if (expiryTime != null) {
                if (System.currentTimeMillis() < expiryTime) {
                    stateWithExpiry.remove(state);  // 일회용 처리
                    log.debug("[OAUTH2] State validated and consumed from memory");
                    return true;
                } else {
                    stateWithExpiry.remove(state);  // 만료된 상태 제거
                    log.warn("[OAUTH2] State validation failed: expired");
                }
            }

            log.warn("[OAUTH2] State validation failed: state not found");
            return false;

        } catch (Exception e) {
            log.error("[OAUTH2] Error during state validation", e);
            return false;
        }
    }
}

