package com.Hamalog.controller.diary;

import com.Hamalog.config.ApiVersion;
import com.Hamalog.dto.diary.request.MoodDiaryCreateRequest;
import com.Hamalog.dto.diary.request.MoodDiaryUpdateRequest;
import com.Hamalog.dto.diary.response.MoodDiaryCalendarResponse;
import com.Hamalog.dto.diary.response.MoodDiaryListResponse;
import com.Hamalog.dto.diary.response.MoodDiaryResponse;
import com.Hamalog.dto.diary.response.MoodDiaryStatsResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import com.Hamalog.service.diary.MoodDiaryService;
import com.Hamalog.service.diary.MoodDiaryStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Mood Diary API", description = "마음 일기 관련 CRUD API")
@RestController
@RequestMapping(ApiVersion.MOOD_DIARY)
@RequiredArgsConstructor
public class MoodDiaryController {

    private final MoodDiaryService moodDiaryService;
    private final MoodDiaryStatsService moodDiaryStatsService;

    @Operation(summary = "마음 일기 생성",
            description = "하루에 1번 작성 가능한 마음 일기를 생성합니다. 템플릿 형식 또는 자유 형식 중 선택 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "마음 일기 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MoodDiaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "해당 날짜에 이미 일기가 존재함", content = @Content)
    })
    @PostMapping
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MOOD_DIARY_BY_MEMBER,
            paramName = "request",
            source = RequireResourceOwnership.ParameterSource.REQUEST_BODY,
            bodyField = "memberId",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MoodDiaryResponse> createMoodDiary(
            @Parameter(description = "마음 일기 생성 요청 데이터", required = true)
            @Valid @RequestBody MoodDiaryCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        if (!memberId.equals(request.memberId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        MoodDiaryResponse response = moodDiaryService.createMoodDiary(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "특정 마음 일기 조회",
            description = "마음 일기 ID로 특정 일기를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마음 일기 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MoodDiaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (다른 사용자의 일기)", content = @Content),
            @ApiResponse(responseCode = "404", description = "마음 일기를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{mood-diary-id}")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MOOD_DIARY,
            paramName = "mood-diary-id"
    )
    public ResponseEntity<MoodDiaryResponse> getMoodDiary(
            @Parameter(description = "마음 일기 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("mood-diary-id") Long moodDiaryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        MoodDiaryResponse response = moodDiaryService.getMoodDiary(moodDiaryId, memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "회원의 모든 마음 일기 조회",
            description = "특정 회원의 모든 마음 일기를 최신 순으로 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마음 일기 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MoodDiaryListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (다른 사용자의 일기)", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/list/{member-id}")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MOOD_DIARY_BY_MEMBER,
            paramName = "member-id",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MoodDiaryListResponse> getMoodDiariesByMember(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long authenticatedMemberId = getAuthenticatedMemberId(userDetails);
        if (!authenticatedMemberId.equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        MoodDiaryListResponse response = moodDiaryService.getMoodDiariesByMember(memberId, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "특정 날짜의 마음 일기 조회",
            description = "특정 회원의 특정 날짜에 작성한 마음 일기를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마음 일기 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MoodDiaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (다른 사용자의 일기)", content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 날짜의 마음 일기를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/date/{member-id}")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MOOD_DIARY_BY_MEMBER,
            paramName = "member-id",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MoodDiaryResponse> getMoodDiaryByDate(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @Parameter(description = "일기 날짜 (yyyy-MM-dd)", required = true, example = "2025-12-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate diaryDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 권한 검증은 @RequireResourceOwnership AOP에서 처리
        MoodDiaryResponse response = moodDiaryService.getMoodDiaryByDate(memberId, diaryDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "마음 일기 수정",
            description = "특정 마음 일기의 내용을 수정합니다. 기분, 일기 형식 및 내용을 변경할 수 있습니다. 날짜는 변경할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마음 일기 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MoodDiaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (다른 사용자의 일기)", content = @Content),
            @ApiResponse(responseCode = "404", description = "마음 일기를 찾을 수 없음", content = @Content)
    })
    @PutMapping("/{mood-diary-id}")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MOOD_DIARY,
            paramName = "mood-diary-id"
    )
    public ResponseEntity<MoodDiaryResponse> updateMoodDiary(
            @Parameter(description = "마음 일기 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("mood-diary-id") Long moodDiaryId,
            @Parameter(description = "마음 일기 수정 요청 데이터", required = true)
            @Valid @RequestBody MoodDiaryUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        MoodDiaryResponse response = moodDiaryService.updateMoodDiary(moodDiaryId, memberId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "마음 일기 삭제",
            description = "특정 마음 일기를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "마음 일기 삭제 성공", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (다른 사용자의 일기)", content = @Content),
            @ApiResponse(responseCode = "404", description = "마음 일기를 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{mood-diary-id}")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MOOD_DIARY,
            paramName = "mood-diary-id"
    )
    public ResponseEntity<Void> deleteMoodDiary(
            @Parameter(description = "마음 일기 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("mood-diary-id") Long moodDiaryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        moodDiaryService.deleteMoodDiary(moodDiaryId, memberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "마음 일기 통계 조회",
            description = "기간별 마음 일기 통계를 조회합니다. 기분 분포, 연속 작성일, 작성률 등을 제공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "통계 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MoodDiaryStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/stats/{member-id}")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MOOD_DIARY_BY_MEMBER,
            paramName = "member-id",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MoodDiaryStatsResponse> getStats(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)", required = true, example = "2025-12-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)", required = true, example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long authenticatedMemberId = getAuthenticatedMemberId(userDetails);
        if (!authenticatedMemberId.equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        MoodDiaryStatsResponse response = moodDiaryStatsService.getStats(memberId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "마음 일기 캘린더 조회",
            description = "월별 마음 일기 캘린더를 조회합니다. 각 날짜별 일기 작성 여부와 기분 유형을 제공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "캘린더 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MoodDiaryCalendarResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/calendar/{member-id}")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MOOD_DIARY_BY_MEMBER,
            paramName = "member-id",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MoodDiaryCalendarResponse> getCalendar(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @Parameter(description = "조회 년도", required = true, example = "2025")
            @RequestParam int year,
            @Parameter(description = "조회 월 (1-12)", required = true, example = "12")
            @RequestParam int month,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long authenticatedMemberId = getAuthenticatedMemberId(userDetails);
        if (!authenticatedMemberId.equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        MoodDiaryCalendarResponse response = moodDiaryStatsService.getCalendar(memberId, year, month);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "마음 일기 검색",
            description = "키워드로 마음 일기 내용을 검색합니다. 자유형식 내용과 템플릿 답변 모두 검색됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MoodDiaryListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/search/{member-id}")
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MOOD_DIARY_BY_MEMBER,
            paramName = "member-id",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public ResponseEntity<MoodDiaryListResponse> searchMoodDiaries(
            @Parameter(description = "회원 ID", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable("member-id") Long memberId,
            @Parameter(description = "검색 키워드", required = true, example = "행복")
            @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long authenticatedMemberId = getAuthenticatedMemberId(userDetails);
        if (!authenticatedMemberId.equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        MoodDiaryListResponse response = moodDiaryService.searchMoodDiaries(memberId, keyword, page, size);
        return ResponseEntity.ok(response);
    }

    private Long getAuthenticatedMemberId(UserDetails userDetails) {
        if (!(userDetails instanceof CustomUserDetails customUserDetails)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        if (customUserDetails.getMember() == null || customUserDetails.getMember().getMemberId() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return customUserDetails.getMember().getMemberId();
    }
}
