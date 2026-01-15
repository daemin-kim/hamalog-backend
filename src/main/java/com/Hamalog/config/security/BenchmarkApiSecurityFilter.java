package com.Hamalog.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 벤치마크 API 보안 필터
 *
 * 프로덕션 환경에서 벤치마크 API가 활성화될 때 추가 보안 계층을 제공합니다.
 * - API Key 헤더 검증
 * - IP 화이트리스트 검증 (선택적)
 *
 * 사용 방법:
 * 1. GitHub Secrets에 BENCHMARK_API_KEY 설정
 * 2. 요청 시 X-Benchmark-API-Key 헤더 포함
 */
@Component
@Profile("benchmark")
@Order(1) // Security 필터 체인보다 먼저 실행
@RequiredArgsConstructor
@Slf4j
public class BenchmarkApiSecurityFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Benchmark-API-Key";
    private static final String BENCHMARK_API_PATH = "/api/v1/benchmark";

    @Value("${hamalog.benchmark.api-key:}")
    private String expectedApiKey;

    @Value("${hamalog.benchmark.allowed-ips:}")
    private String allowedIpsConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // 벤치마크 API 경로가 아니면 통과
        if (!requestPath.startsWith(BENCHMARK_API_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        // API Key가 설정되어 있으면 검증
        if (StringUtils.hasText(expectedApiKey)) {
            String providedApiKey = request.getHeader(API_KEY_HEADER);

            if (!expectedApiKey.equals(providedApiKey)) {
                log.warn("[BENCHMARK SECURITY] API Key 검증 실패 - IP: {}, Path: {}",
                        getClientIp(request), requestPath);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or missing benchmark API key\"}");
                return;
            }
        }

        // IP 화이트리스트가 설정되어 있으면 검증
        if (StringUtils.hasText(allowedIpsConfig)) {
            Set<String> allowedIps = Arrays.stream(allowedIpsConfig.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());

            String clientIp = getClientIp(request);

            if (!allowedIps.contains(clientIp)) {
                log.warn("[BENCHMARK SECURITY] IP 화이트리스트 검증 실패 - IP: {}, Path: {}",
                        clientIp, requestPath);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"IP not allowed for benchmark API\"}");
                return;
            }
        }

        log.info("[BENCHMARK] 요청 허용 - IP: {}, Path: {}", getClientIp(request), requestPath);
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // Cloudflare를 통해 들어오는 경우 실제 클라이언트 IP 확인
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (StringUtils.hasText(cfConnectingIp)) {
            return cfConnectingIp;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}

