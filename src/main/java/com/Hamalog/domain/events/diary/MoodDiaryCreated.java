package com.Hamalog.domain.events.diary;

import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodType;
import com.Hamalog.domain.events.DomainEvent;
import java.time.LocalDate;
import lombok.Getter;

/**
 * 마음 일기 생성 이벤트
 * 일기가 생성될 때 발행되어 통계 캐시 갱신 및 연속 작성일 추적에 사용
 */
@Getter
public class MoodDiaryCreated extends DomainEvent {

    private final Long moodDiaryId;
    private final Long memberId;
    private final String memberLoginId;
    private final LocalDate diaryDate;
    private final MoodType moodType;
    private final DiaryType diaryType;
    private final Integer consecutiveDays;

    public MoodDiaryCreated(
            Long moodDiaryId,
            Long memberId,
            String memberLoginId,
            LocalDate diaryDate,
            MoodType moodType,
            DiaryType diaryType,
            Integer consecutiveDays
    ) {
        super();
        this.moodDiaryId = moodDiaryId;
        this.memberId = memberId;
        this.memberLoginId = memberLoginId;
        this.diaryDate = diaryDate;
        this.moodType = moodType;
        this.diaryType = diaryType;
        this.consecutiveDays = consecutiveDays;
    }

    @Override
    public String getAggregateId() {
        return moodDiaryId.toString();
    }

    /**
     * 부정적인 기분인지 확인 (부작용 기록 권유 대상)
     */
    public boolean isNegativeMood() {
        return moodType == MoodType.ANXIOUS ||
               moodType == MoodType.SAD ||
               moodType == MoodType.ANGRY ||
               moodType == MoodType.LETHARGIC;
    }

    @Override
    public String toString() {
        return String.format(
                "MoodDiaryCreated{diaryId=%d, memberId=%d, date=%s, mood=%s, occurredOn=%s}",
                moodDiaryId, memberId, diaryDate, moodType, getOccurredOn()
        );
    }
}
