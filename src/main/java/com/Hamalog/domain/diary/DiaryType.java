package com.Hamalog.domain.diary;
import lombok.Getter;
@Getter
public enum DiaryType {
    TEMPLATE("템플릿 형식 (4개의 질문)"),
    FREE_FORM("자유 형식 (텍스트 박스 1개)");
    private final String description;
    DiaryType(String description) {
        this.description = description;
    }
}
