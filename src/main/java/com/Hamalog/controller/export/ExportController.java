package com.Hamalog.controller.export;

import com.Hamalog.config.ApiVersion;
import com.Hamalog.dto.export.ExportDataResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.service.export.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 데이터 내보내기 Controller
 *
 * 사용자 데이터를 JSON 또는 CSV 형식으로 내보내는 기능을 제공합니다.
 */
@Tag(name = "Export API", description = "데이터 내보내기 API (JSON, CSV)")
@RestController
@RequestMapping(ApiVersion.EXPORT)
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "내 데이터 내보내기 (JSON 객체)",
            description = "현재 로그인한 사용자의 모든 데이터를 JSON 객체로 반환합니다. " +
                    "회원 정보, 복약 스케줄, 복약 기록, 마음 일기, 부작용 기록이 포함됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내보내기 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ExportDataResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/my-data")
    public ResponseEntity<ExportDataResponse> exportMyData(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        log.info("데이터 내보내기 요청 - memberId: {}", memberId);

        ExportDataResponse response = exportService.exportAllData(memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 데이터 JSON 파일 다운로드",
            description = "현재 로그인한 사용자의 모든 데이터를 JSON 파일로 다운로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "다운로드 성공",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/my-data/download")
    public ResponseEntity<byte[]> downloadMyDataAsJson(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);
        log.info("데이터 JSON 파일 다운로드 요청 - memberId: {}", memberId);

        String jsonData = exportService.exportAsJson(memberId);
        byte[] bytes = jsonData.getBytes(StandardCharsets.UTF_8);

        String filename = String.format("hamalog_data_%s_%s.json",
                memberId,
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }

    @Operation(summary = "복약 기록 CSV 내보내기",
            description = "지정된 기간의 복약 기록을 CSV 파일로 내보냅니다. 의사 상담 시 활용할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내보내기 성공",
                    content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/medication-records")
    public ResponseEntity<byte[]> exportMedicationRecordsCsv(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)", example = "2025-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)", example = "2025-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long memberId = getAuthenticatedMemberId(userDetails);

        // 기본값: 최근 3개월
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusMonths(3);
        }

        log.info("복약 기록 CSV 내보내기 요청 - memberId: {}, startDate: {}, endDate: {}",
                memberId, startDate, endDate);

        String csvData = exportService.exportMedicationRecordsAsCsv(memberId, startDate, endDate);
        // UTF-8 BOM 추가 (Excel 한글 호환성)
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = csvData.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, bytes, 0, bom.length);
        System.arraycopy(csvBytes, 0, bytes, bom.length, csvBytes.length);

        String filename = String.format("medication_records_%s_%s_to_%s.csv",
                memberId,
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }

    private Long getAuthenticatedMemberId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getMember() == null
                || userDetails.getMember().getMemberId() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getMember().getMemberId();
    }
}
