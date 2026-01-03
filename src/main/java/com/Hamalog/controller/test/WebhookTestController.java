package com.Hamalog.controller.test;

import com.Hamalog.handler.ErrorSeverity;
import com.Hamalog.service.alert.DiscordAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Discord 웹훅 알림 테스트용 컨트롤러
 * local, dev 환경에서만 활성화됩니다.
 */
@RestController
@RequestMapping("/api/v1/test")
@Profile({"local", "dev"})
@Tag(name = "테스트", description = "개발 환경 테스트용 API")
public class WebhookTestController {

    private final DiscordAlertService discordAlertService;

    public WebhookTestController(
            @Autowired(required = false) DiscordAlertService discordAlertService
    ) {
        this.discordAlertService = discordAlertService;
    }

    /**
     * HIGH 심각도 에러를 발생시켜 Discord 알림 테스트
     */
    @GetMapping("/webhook/high")
    @Operation(summary = "HIGH 심각도 Discord 알림 테스트")
    public ResponseEntity<String> testHighSeverityAlert(HttpServletRequest request) {
        if (discordAlertService == null) {
            return ResponseEntity.badRequest()
                    .body("Discord 알림 서비스가 비활성화되어 있습니다. " +
                            "hamalog.alert.discord.enabled=true 설정이 필요합니다.");
        }

        RuntimeException testException = new RuntimeException("Discord 웹훅 테스트 - HIGH 심각도");
        discordAlertService.sendServerErrorAlert(testException, request, ErrorSeverity.HIGH);
        return ResponseEntity.ok("HIGH 심각도 알림 발송 요청 완료 (비동기 처리)");
    }

    /**
     * CRITICAL 심각도 에러를 발생시켜 Discord 알림 테스트
     */
    @GetMapping("/webhook/critical")
    @Operation(summary = "CRITICAL 심각도 Discord 알림 테스트")
    public ResponseEntity<String> testCriticalSeverityAlert(HttpServletRequest request) {
        if (discordAlertService == null) {
            return ResponseEntity.badRequest()
                    .body("Discord 알림 서비스가 비활성화되어 있습니다. " +
                            "hamalog.alert.discord.enabled=true 설정이 필요합니다.");
        }

        RuntimeException testException = new RuntimeException("Discord 웹훅 테스트 - CRITICAL 심각도");
        discordAlertService.sendServerErrorAlert(testException, request, ErrorSeverity.CRITICAL);
        return ResponseEntity.ok("CRITICAL 심각도 알림 발송 요청 완료 (비동기 처리)");
    }

    /**
     * 실제 500 에러를 발생시켜 GlobalExceptionHandler 경유 테스트
     */
    @GetMapping("/error/500")
    @Operation(summary = "500 에러 발생 테스트 (GlobalExceptionHandler 경유)")
    public ResponseEntity<String> trigger500Error(
            @RequestParam(defaultValue = "웹훅 테스트용 500 에러") String message) {
        throw new RuntimeException(message);
    }

    /**
     * Discord 알림 서비스 상태 확인
     */
    @GetMapping("/webhook/status")
    @Operation(summary = "Discord 알림 서비스 활성화 상태 확인")
    public ResponseEntity<String> checkWebhookStatus() {
        if (discordAlertService == null) {
            return ResponseEntity.ok("Discord 알림 서비스: 비활성화 (hamalog.alert.discord.enabled=false)");
        }
        return ResponseEntity.ok("Discord 알림 서비스: 활성화");
    }
}
