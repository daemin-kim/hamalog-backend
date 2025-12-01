package com.Hamalog.repository.diary;
import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;
@Repository
public interface MoodDiaryRepository extends JpaRepository<MoodDiary, Long> {
    Optional<MoodDiary> findByMemberAndDiaryDate(Member member, LocalDate diaryDate);
    boolean existsByMemberAndDiaryDate(Member member, LocalDate diaryDate);
    Page<MoodDiary> findByMemberOrderByDiaryDateDesc(Member membpackage com.Hamalog.repository.diary;
import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageablmbimport com.Hamalog.domain.diary.MoodDiimport com.Hamalog.domain.member.Member;

}import ormkdir -p /Users/daeminkim/ideaProjects/Hamalog/src/main/java/com/Hamalog/dto/diary/request && mkdir -p /Users/daeminkim/ideaProjects/Hamalog/src/main/java/com/Hamalog/dto/diary/response
cat << 'EOFFILE' > /Users/daeminkim/ideaProjects/Hamalog/src/main/java/com/Hamalog/dto/diary/request/MoodDiaryCreateRequest.java
package com.Hamalog.dto.diary.request;
import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MoodDiaryCreateRequest {
    @NotNull(message = "{moodDiary.memberId.notNull}")
    private Long memberId;
    @NotNull(message = "{moodDiary.diaryDate.notNull}")
    private LocalDate diaryDate;
    @NotNull(message = "{moodDiary.moodType.notNull}")
    private MoodType moodType;
    @NotNull(message = "{moodDiary.diaryType.notNull}")
    private DiaryType diaryType;
    @Size(max = 500, message = "{moodDiary.templateAnswer1.size}")
    private String templateAnswer1;
    @Size(max = 500, message = "{moodDiary.templateAnswer2.size}")
    private String templateAnswer2;
    @Sipackage com.Hamalog.dto.diary.request;
import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodType;
imp"{import com.Hamalog.domain.diary.Diary pimport com.Hamalog.domain.diary.MoodType;maimport jakarta.validation.constraints.*;enimport lombok.*;
import java.time.Localt;import java.timle@Getter
@Builder
@NoArgsCo
 @Build i@NoArgsyT@AllArgsConstructor
public class MoodDiaryCreateRrupublic class MoodD      @NotNull(message = "{moodDiary.mte    private Long memberId;
    @NotNull(message = "{msw    @NotNull(message = "{ns    private LocalDate diaryDate;
    @NotNull(message ul    @NotNull(message = "{moodDi
     private MoodType moodType;
    @NotNull(message =eA    @NotNull(message = "{moodub    private DiaryType diaryType;
    @Size(max = 500, yp    @DiaryType.FREE_FORM) {
         private String templateAnswer1;
    @Size(max = 500, message &&    @Size(max = 500, message = "{mOF    pcat << 'EOFFILE' > /Users/daeminkim/ideaProjects/Hamalog/src/main/java/com/Hamalog/dto/diary/response/MoodDiaryResponse.java
package com.Hamalog.dto.diary.response;
import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.diary.MoodType;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MoodDiaryResponse {
    private Long moodDiaryId;
    private Long memberId;
    private LocalDate diaryDate;
    private MoodType moodType;
    private DiaryType diaryType;
    private String templateAnswer1;
    private String templateAnswer2;
    private String templateAnswer3;
    private String templateAnswer4;
    private String freeContent;
    private LocalDateTime createdAt;
    public static MoodDiaryResponse from(MoodDiary moodDiary) {
        return MoodDiaryResponse.builder()
                .moodDiaryId(moodDiary.getMoodDiaryId())
  package com.Hamalog.dto.diary.response;
import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodDiar  import com.Hamalog.domain.diary.DiaryT)
import com.Hamalog.domain.diary.MoodDiaryiaimport com.Hamalog.domain.diary.MoodType;1(import lombok.*;
import java.time.LocalD  import java.timAnimport java.time.LocalDateeA@Getter
@B                .templ@Buildwe@NoArgsDi@AllArgsConstructor
public class MoodDiaryResponstepublic class MoodDge    private Long moodDiaryId;
       private Long memberId;
 tF    private LocalDate dia      private MoodType moodType;
te    private DiaryType diaryTy()    private Stringcat << 'EOFFILE' > /Users/daeminkim/ideaProjects/Hamalog/src/main/java/com/Hamalog/dto/diary/response/MoodDiaryListResponse.java
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
