package com.Hamalog.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 악성 봇 및 취약점 스캐너를 차단하는 보안 필터
 *
 * 이 필터는 다음과 같은 공격을 방어합니다:
 * - 알려진 취약점 스캐너 (l9scan, Leakix 등)
 * - 의심스러운 경로 접근 시도 (/.env, /.git, /swagger 등)
 * - 자동화된 봇 트래픽
 */
@Slf4j
@Component
public class BotProtectionFilter extends OncePerRequestFilter {

    private final TrustedProxyService trustedProxyService;
    private final boolean enabled;
    private final boolean logBlockedRequests;

    // 차단된 IP별 카운터 (메모리 기반, 재시작시 초기화)
    private final ConcurrentHashMap<String, AtomicInteger> blockedIpCounter = new ConcurrentHashMap<>();

    // 알려진 악성 봇/스캐너 User-Agent 패턴
    private static final List<Pattern> BLOCKED_USER_AGENT_PATTERNS = List.of(
        // 취약점 스캐너
        Pattern.compile("(?i).*l9scan.*"),
        Pattern.compile("(?i).*leakix.*"),
        Pattern.compile("(?i).*zgrab.*"),
        Pattern.compile("(?i).*nuclei.*"),
        Pattern.compile("(?i).*nikto.*"),
        Pattern.compile("(?i).*sqlmap.*"),
        Pattern.compile("(?i).*nmap.*"),
        Pattern.compile("(?i).*masscan.*"),
        Pattern.compile("(?i).*dirbuster.*"),
        Pattern.compile("(?i).*gobuster.*"),
        Pattern.compile("(?i).*wfuzz.*"),
        Pattern.compile("(?i).*burpsuite.*"),
        Pattern.compile("(?i).*acunetix.*"),
        Pattern.compile("(?i).*nessus.*"),
        Pattern.compile("(?i).*openvas.*"),
        Pattern.compile("(?i).*qualys.*"),
        Pattern.compile("(?i).*censys.*"),
        Pattern.compile("(?i).*shodan.*"),
        Pattern.compile("(?i).*cybex\\.ai.*"),
        Pattern.compile("(?i).*cypex\\.ai.*"),

        // 일반적인 봇/크롤러 (검색엔진 제외)
        Pattern.compile("(?i)^Go-http-client.*"),
        Pattern.compile("(?i).*python-requests.*"),
        Pattern.compile("(?i).*curl/.*"),
        Pattern.compile("(?i).*wget.*"),
        Pattern.compile("(?i).*libwww-perl.*"),
        Pattern.compile("(?i).*httpclient.*"),
        Pattern.compile("(?i).*java/.*"),

        // 보안 스캐너 (허용된 경우 제외)
        Pattern.compile("(?i).*Cortex-Xpanse.*"),
        Pattern.compile("(?i).*Palo Alto.*")
    );

    // 허용된 봇 User-Agent (검색엔진 등)
    private static final Set<String> ALLOWED_BOT_PATTERNS = Set.of(
        "googlebot",
        "bingbot",
        "yandexbot",
        "duckduckbot",
        "slurp",
        "baiduspider",
        "facebookexternalhit",
        "twitterbot",
        "linkedinbot",
        "applebot"
    );

    // 의심스러운 경로 패턴 (취약점 스캐닝에 자주 사용됨)
    private static final List<Pattern> SUSPICIOUS_PATH_PATTERNS = List.of(
        // 설정 파일 탈취 시도
        Pattern.compile("(?i).*/\\.env.*"),
        Pattern.compile("(?i).*/\\.git.*"),
        Pattern.compile("(?i).*/\\.svn.*"),
        Pattern.compile("(?i).*/\\.hg.*"),
        Pattern.compile("(?i).*/\\.DS_Store.*"),
        Pattern.compile("(?i).*/\\.vscode.*"),
        Pattern.compile("(?i).*/\\.idea.*"),
        Pattern.compile("(?i).*/config\\.json.*"),
        Pattern.compile("(?i).*/sftp\\.json.*"),
        Pattern.compile("(?i).*/wp-config\\.php.*"),

        // 관리자/디버그 페이지 접근 시도
        Pattern.compile("(?i).*/wp-admin.*"),
        Pattern.compile("(?i).*/wp-login.*"),
        Pattern.compile("(?i).*/phpmyadmin.*"),
        Pattern.compile("(?i).*/adminer.*"),
        Pattern.compile("(?i).*/debug.*"),
        Pattern.compile("(?i).*/console.*"),
        Pattern.compile("(?i).*/server-status.*"),
        Pattern.compile("(?i).*/telescope.*"),

        // 알려진 취약점 경로
        Pattern.compile("(?i).*/graphql.*"),  // 애플리케이션에서 사용하지 않음
        Pattern.compile("(?i).*/_all_dbs.*"),  // CouchDB
        Pattern.compile("(?i).*/v2/_catalog.*"),  // Docker Registry
        Pattern.compile("(?i).*/ecp/.*"),  // Exchange
        Pattern.compile("(?i).*/geoserver.*"),
        Pattern.compile("(?i).*/META-INF.*"),
        Pattern.compile("(?i).*/WEB-INF.*"),

        // PHP/ASP 파일 접근 시도 (Java 애플리케이션)
        Pattern.compile("(?i).*\\.php$"),
        Pattern.compile("(?i).*\\.asp$"),
        Pattern.compile("(?i).*\\.aspx$"),
        Pattern.compile("(?i).*\\.jsp$")  // 우리 앱은 REST API만 제공
    );

    public BotProtectionFilter(
            TrustedProxyService trustedProxyService,
            @Value("${hamalog.security.bot-protection.enabled:true}") boolean enabled,
            @Value("${hamalog.security.bot-protection.log-blocked:true}") boolean logBlockedRequests
    ) {
        this.trustedProxyService = trustedProxyService;
        this.enabled = enabled;
        this.logBlockedRequests = logBlockedRequests;

        log.info("[BOT_PROTECTION] Filter initialized - enabled: {}, logBlocked: {}", enabled, logBlockedRequests);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // 1. User-Agent 기반 차단
        BlockReason blockReason = checkUserAgent(userAgent);
        if (blockReason != null) {
            handleBlockedRequest(response, clientIp, userAgent, requestUri, method, blockReason);
            return;
        }

        // 2. 의심스러운 경로 차단
        blockReason = checkSuspiciousPath(requestUri);
        if (blockReason != null) {
            handleBlockedRequest(response, clientIp, userAgent, requestUri, method, blockReason);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private BlockReason checkUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            // 빈 User-Agent는 봇일 가능성이 높지만, 일부 정상 클라이언트도 있을 수 있음
            return null;  // 일단 허용
        }

        String lowerUserAgent = userAgent.toLowerCase();

        // 허용된 봇인지 먼저 확인
        for (String allowedPattern : ALLOWED_BOT_PATTERNS) {
            if (lowerUserAgent.contains(allowedPattern)) {
                return null;
            }
        }

        // 차단 패턴 확인
        for (Pattern pattern : BLOCKED_USER_AGENT_PATTERNS) {
            if (pattern.matcher(userAgent).matches()) {
                return new BlockReason("BLOCKED_USER_AGENT", pattern.pattern());
            }
        }

        return null;
    }

    private BlockReason checkSuspiciousPath(String requestUri) {
        if (requestUri == null) {
            return null;
        }

        for (Pattern pattern : SUSPICIOUS_PATH_PATTERNS) {
            if (pattern.matcher(requestUri).matches()) {
                return new BlockReason("SUSPICIOUS_PATH", pattern.pattern());
            }
        }

        return null;
    }

    private void handleBlockedRequest(HttpServletResponse response, String clientIp,
                                      String userAgent, String requestUri, String method,
                                      BlockReason reason) throws IOException {

        // 차단 카운터 증가
        blockedIpCounter.computeIfAbsent(clientIp, k -> new AtomicInteger(0)).incrementAndGet();

        if (logBlockedRequests) {
            log.warn("[BOT_PROTECTION] Blocked request - IP: {}, Reason: {} ({}), URI: {}, Method: {}, User-Agent: {}",
                    clientIp, reason.type(), reason.pattern(), requestUri, method,
                    userAgent != null ? userAgent.substring(0, Math.min(100, userAgent.length())) : "null");
        }

        // 403 Forbidden 응답
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 최소한의 정보만 반환 (공격자에게 힌트 제공 방지)
        response.getWriter().write("{\"error\":\"Forbidden\",\"status\":403}");
    }

    private String getClientIp(HttpServletRequest request) {
        return trustedProxyService.resolveClientIp(request)
                .orElse(request.getRemoteAddr());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        // 헬스체크와 정적 리소스는 필터 제외
        return requestUri.equals("/actuator/health") ||
               requestUri.startsWith("/static/") ||
               requestUri.equals("/favicon.ico");
    }

    /**
     * 차단 통계 조회 (관리/모니터링 용도)
     */
    public ConcurrentHashMap<String, AtomicInteger> getBlockedIpStats() {
        return blockedIpCounter;
    }

    /**
     * 차단 통계 초기화
     */
    public void clearBlockedIpStats() {
        blockedIpCounter.clear();
    }

    private record BlockReason(String type, String pattern) {}
}

