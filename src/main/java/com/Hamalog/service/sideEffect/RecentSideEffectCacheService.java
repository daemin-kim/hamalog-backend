package com.Hamalog.service.sideEffect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Redis를 사용하여 최근 부작용 목록을 캐싱하는 서비스
 * 사용자별로 최대 5개의 최근 부작용 이름을 저장합니다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RecentSideEffectCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "recent_side_effects:";
    private static final long MAX_RECENT_ITEMS = 5;

    public RecentSideEffectCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 사용자의 최근 부작용 목록을 Redis에서 조회합니다.
     *
     * @param memberId 사용자 ID
     * @return 최근 부작용 이름 목록 (최대 5개)
     */
    public List<String> getRecentSideEffects(Long memberId) {
        try {
            String key = getCacheKey(memberId);
            List<Object> cachedItems = redisTemplate.opsForList().range(key, 0, MAX_RECENT_ITEMS - 1);
            
            if (cachedItems == null || cachedItems.isEmpty()) {
                return Collections.emptyList();
            }
            
            return cachedItems.stream()
                    .map(Object::toString)
                    .toList();
        } catch (Exception e) {
            // Redis 오류 시 빈 리스트 반환 (데이터베이스 폴백 허용)
            log.warn("Failed to get recent side effects from Redis cache for member {}: {}", memberId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 새로운 부작용을 Redis 캐시에 추가합니다.
     * 리스트의 앞쪽에 추가하고 최대 5개까지만 유지합니다.
     *
     * @param memberId 사용자 ID
     * @param sideEffectName 부작용 이름
     */
    public void addRecentSideEffect(Long memberId, String sideEffectName) {
        try {
            String key = getCacheKey(memberId);
            
            // 중복 제거: 기존에 같은 부작용이 있다면 먼저 제거
            redisTemplate.opsForList().remove(key, 0, sideEffectName);
            
            // 리스트 앞쪽에 새 항목 추가
            redisTemplate.opsForList().leftPush(key, sideEffectName);
            
            // 최대 5개까지만 유지 (오래된 항목 제거)
            redisTemplate.opsForList().trim(key, 0, MAX_RECENT_ITEMS - 1);
            
            // TTL 설정 (30일 후 만료)
            redisTemplate.expire(key, java.time.Duration.ofDays(30));
            
        } catch (Exception e) {
            // Redis 오류 시 로그만 기록하고 계속 진행
            log.error("Failed to add recent side effect '{}' to Redis cache for member {}: {}", 
                     sideEffectName, memberId, e.getMessage(), e);
        }
    }

    /**
     * 사용자의 최근 부작용 캐시를 초기화합니다.
     *
     * @param memberId 사용자 ID
     * @param sideEffectNames 초기화할 부작용 이름 목록
     */
    public void refreshRecentSideEffects(Long memberId, List<String> sideEffectNames) {
        try {
            String key = getCacheKey(memberId);
            
            // 기존 캐시 삭제
            redisTemplate.delete(key);
            
            // 새 데이터로 캐시 초기화 (최대 5개)
            List<String> limitedList = sideEffectNames.stream()
                    .limit(MAX_RECENT_ITEMS)
                    .toList();
            
            if (!limitedList.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(key, limitedList.toArray());
                redisTemplate.expire(key, java.time.Duration.ofDays(30));
            }
            
        } catch (Exception e) {
            // Redis 오류 시 로그만 기록하고 계속 진행
            log.error("Failed to refresh recent side effects cache for member {}: {}", 
                     memberId, e.getMessage(), e);
        }
    }

    /**
     * Redis에서 사용할 캐시 키를 생성합니다.
     *
     * @param memberId 사용자 ID
     * @return Redis 캐시 키
     */
    private String getCacheKey(Long memberId) {
        return CACHE_KEY_PREFIX + memberId;
    }
}