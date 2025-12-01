package com.Hamalog.domain.diary;
import lombok.Getter;
@Getter
public enum MoodType {
    HAPPY("행복"),
    EXCITED("신남"),
    PEACEFUL("평온"),
    ANXIOUS("불안&긴장"),
    LETHARGIC("무기력"),
    ANGRY("분노"),
    SAD("슬픔");
    private final String description;
    MoodType(String description) {
        this.description = description;
    }
}
