package com.Hamalog.dto.diary.response;

import java.util.List;
import lombok.*;

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
