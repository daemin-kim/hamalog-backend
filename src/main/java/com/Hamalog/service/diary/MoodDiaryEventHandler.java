package com.Hamalog.service.diary;

import com.Hamalog.domain.events.diary.MoodDiaryCreated;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.BusinessEvent;
import com.Hamalog.service.notification.FcmPushService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 마음 일기 이벤트 핸들러
 * 캐시 무효화(동기)와 알림/분석(비동기)를 분리하여 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MoodDiaryEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StructuredLogger structuredLogger;
    private final FcmPushService fcmPushService;

    /**
     * 마음 일기 생성 시 캐시 무효화 (동기 처리)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCacheInvalidation(MoodDiaryCreated event) {
        log.debug("Invalidating caches for MoodDiaryCreated: {}", event.getMoodDiaryId());

        try {
            // 일기 통계 캐시 무효화
            String statsKey = "mood_diary_stats:" + event.getMemberId();
            String calendarKey = "mood_diary_calendar:" + event.getMemberId() + ":" +
                    event.getDiaryDate().getYear() + ":" + event.getDiaryDate().getMonthValue();
            String consecutiveKey = "mood_diary_consecutive:" + event.getMemberId();

            redisTemplate.delete(statsKey);
            redisTemplate.delete(calendarKey);
            redisTemplate.delete(consecutiveKey);

            log.debug("Invalidated mood diary caches for memberId: {}", event.getMemberId());

        } catch (Exception e) {
            log.error("Failed to invalidate caches for MoodDiaryCreated: {}",
                    event.getMoodDiaryId(), e);
        }
    }

    /**
     * 마음 일기 생성 시 비즈니스 로깅 및 분석 (비동기)
     */
    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAsyncProcessing(MoodDiaryCreated event) {
        log.debug("Async processing for MoodDiaryCreated: {}", event.getMoodDiaryId());

        try {
            // 비즈니스 이벤트 로깅
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("diaryDate", event.getDiaryDate().toString());
            metadata.put("moodType", event.getMoodType().name());
            metadata.put("diaryType", event.getDiaryType().name());
            if (event.getConsecutiveDays() != null) {
                metadata.put("consecutiveDays", event.getConsecutiveDays().toString());
            }

            BusinessEvent businessEvent = BusinessEvent.builder()
                    .eventType("MOOD_DIARY_CREATED")
                    .userId(event.getMemberLoginId())
                    .entity("MoodDiary")
                    .action("CREATED")
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();

            structuredLogger.business(businessEvent);

            // 부정적인 기분이 지속되는 경우 부작용 기록 권유 알림
            if (event.isNegativeMood()) {
                log.info("Negative mood detected for memberId: {}, mood: {} - Consider side effect recording notification",
                        event.getMemberId(), event.getMoodType());

                // 연속 부정적 기분 일수 확인 (연속 3일 이상)
                Integer consecutiveNegativeDays = checkConsecutiveNegativeMoodDays(event.getMemberId());
                if (consecutiveNegativeDays != null && consecutiveNegativeDays >= 3) {
                    fcmPushService.sendNegativeMoodAlert(event.getMemberId(), consecutiveNegativeDays);
                    fcmPushService.sendSideEffectRecordReminder(event.getMemberId(),
                            "최근 며칠간 기분이 좋지 않으셨군요. 혹시 부작용으로 인한 것은 아닌지 확인해보세요.");
                }
            }

            // 연속 작성일 업적 알림
            if (event.getConsecutiveDays() != null && event.getConsecutiveDays() > 0) {
                int days = event.getConsecutiveDays();
                if (days == 7 || days == 30 || days == 100) {
                    log.info("Consecutive days milestone reached for memberId: {}, days: {}",
                            event.getMemberId(), days);
                    fcmPushService.sendConsecutiveDiaryAchievement(event.getMemberId(), days);
                }
            }

            log.info("Completed async processing for MoodDiaryCreated: diaryId={}, memberId={}",
                    event.getMoodDiaryId(), event.getMemberId());

        } catch (Exception e) {
            log.error("Failed async processing for MoodDiaryCreated: {}",
                    event.getMoodDiaryId(), e);
        }
    }

    /**
     * 연속 부정적 기분 일수 확인 (Redis 캐시 기반)
     * 실제 구현에서는 DB 조회가 필요하지만, 간단히 Redis 카운터 사용
     */
    private Integer checkConsecutiveNegativeMoodDays(Long memberId) {
        String key = "negative_mood_streak:" + memberId;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        } catch (Exception e) {
            log.debug("Failed to get negative mood streak from Redis: {}", e.getMessage());
        }
        return 0;
    }
}
