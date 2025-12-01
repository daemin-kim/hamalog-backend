package com.Hamalog.dto.diary.response;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MoodDiaryListResponse {

    private List<MoodDiaryResponse> diaries;
    private long totalCount;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}

