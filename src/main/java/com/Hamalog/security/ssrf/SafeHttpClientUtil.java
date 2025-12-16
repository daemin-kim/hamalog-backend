package com.Hamalog.security.ssrf;

import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * SSRF 공격을 방어하는 안전한 HTTP 클라이언트 유틸리티
 * OWASP Top 10 A10: Server-Side Request Forgery (SSRF) 방어
 */
@Component
public class SafeHttpClientUtil {

    private static final Logger log = LoggerFactory.getLogger(SafeHttpClientUtil.class);

    // 허용된 도메인 화이트리스트 (설정에서 주입)
    @Value("${hamalog.security.allowed-domains:kauth.kakao.com,kapi.kakao.com}")
    private String allowedDomainsConfig;

    // 허용된 스키마
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    // 차단할 내부 IP 대역
    private static final List<String> BLOCKED_IP_RANGES = Arrays.asList(
        "127.0.0.0/8",     // Loopback
        "10.0.0.0/8",      // Private Class A
        "172.16.0.0/12",   // Private Class B
        "192.168.0.0/16",  // Private Class C
        "169.254.0.0/16",  // Link-local
        "224.0.0.0/4",     // Multicast
        "240.0.0.0/4",     // Reserved
        "0.0.0.0/8",       // "This" network
        "100.64.0.0/10",   // Carrier-grade NAT
        "203.0.113.0/24",  // Test network
        "233.252.0.0/24"   // MCAST-TEST-NET
    );

    // 차단할 포트
    private static final Set<Integer> BLOCKED_PORTS = Set.of(
        22, 23, 25, 53, 110, 143, 993, 995, // SSH, Telnet, SMTP, DNS, POP3, IMAP
        135, 139, 445, 1433, 1521, 3306, 5432, 6379, 27017 // Windows, SQL, Redis, MongoDB
    );

    // 허용된 포트 (HTTP/HTTPS 기본값)
    private static final Set<Integer> ALLOWED_PORTS = Set.of(80, 443, 8080, 8443);

    // URL 패턴 검증
    private static final Pattern VALID_URL_PATTERN = Pattern.compile(
        "^https?://[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*(?::[1-9]\\d{0,4})?(?:/[^\\s]*)?$"
    );

    private final RestTemplate restTemplate;

    public SafeHttpClientUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 안전한 HTTP GET 요청
     */
    public <T> ResponseEntity<T> safeGet(String url, Class<T> responseType) {
        return safeRequest(url, HttpMethod.GET, null, responseType);
    }

    /**
     * 안전한 HTTP POST 요청
     */
    public <T> ResponseEntity<T> safePost(String url, Object request, Class<T> responseType) {
        return safeRequest(url, HttpMethod.POST, request, responseType);
    }

    /**
     * 안전한 HTTP 요청 (내부 메서드)
     */
    private <T> ResponseEntity<T> safeRequest(String url, HttpMethod method, Object request, Class<T> responseType) {
        try {
            // URL 유효성 검증
            validateUrl(url);
            
            log.info("안전한 HTTP 요청 시작: {} {}", method, sanitizeUrlForLog(url));
            
            ResponseEntity<T> response;
            if (method == HttpMethod.GET) {
                response = restTemplate.getForEntity(url, responseType);
            } else if (method == HttpMethod.POST) {
                response = restTemplate.postForEntity(url, request, responseType);
            } else {
                throw new SsrfValidationException("지원하지 않는 HTTP 메서드: " + method);
            }
            
            log.info("HTTP 요청 완료: {} {} -> {}", method, sanitizeUrlForLog(url), response.getStatusCode());
            return response;
            
        } catch (Exception e) {
            log.error("안전한 HTTP 요청 실패: {} {} - {}", method, sanitizeUrlForLog(url), e.getMessage());
            throw new SsrfValidationException("HTTP 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * URL 유효성 검증 (SSRF 방어)
     */
    public void validateUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new SsrfValidationException("URL이 비어있습니다");
        }

        // 기본 URL 패턴 검증
        if (!VALID_URL_PATTERN.matcher(url).matches()) {
            throw new SsrfValidationException("유효하지 않은 URL 형식입니다");
        }

        try {
            URI uri = new URI(url);
            URL parsedUrl = uri.toURL();

            // 스키마 검증
            validateScheme(parsedUrl.getProtocol());

            // 도메인 화이트리스트 검증
            validateDomain(parsedUrl.getHost());

            // 포트 검증
            validatePort(parsedUrl.getPort(), parsedUrl.getProtocol());

            // IP 주소 검증 (내부 네트워크 차단)
            validateIpAddress(parsedUrl.getHost());

            log.debug("URL 검증 통과: {}", sanitizeUrlForLog(url));

        } catch (URISyntaxException | MalformedURLException e) {
            throw new SsrfValidationException("URL 파싱 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 스키마 검증
     */
    private void validateScheme(String scheme) {
        if (!ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new SsrfValidationException("허용되지 않은 스키마: " + scheme);
        }
    }

    /**
     * 도메인 화이트리스트 검증
     */
    private void validateDomain(String host) {
        if (!StringUtils.hasText(host)) {
            throw new SsrfValidationException("호스트가 비어있습니다");
        }

        Set<String> allowedDomains = getAllowedDomains();
        String lowerHost = host.toLowerCase();

        // 정확한 도메인 매치 또는 서브도메인 매치
        boolean isAllowed = allowedDomains.stream()
            .anyMatch(domain -> lowerHost.equals(domain) || lowerHost.endsWith("." + domain));

        if (!isAllowed) {
            throw new SsrfValidationException("허용되지 않은 도메인: " + host);
        }
    }

    /**
     * 포트 검증
     */
    private void validatePort(int port, String scheme) {
        // 기본 포트 처리
        if (port == -1) {
            port = "https".equals(scheme) ? 443 : 80;
        }

        // 차단된 포트 확인
        if (BLOCKED_PORTS.contains(port)) {
            throw new SsrfValidationException("차단된 포트: " + port);
        }

        // 허용된 포트 확인
        if (!ALLOWED_PORTS.contains(port)) {
            throw new SsrfValidationException("허용되지 않은 포트: " + port);
        }
    }

    /**
     * IP 주소 검증 (내부 네트워크 차단)
     */
    private void validateIpAddress(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            
            // 내부 IP 대역 검증
            if (isInternalIp(address)) {
                throw new SsrfValidationException("내부 네트워크 접근이 차단되었습니다: " + address.getHostAddress());
            }

            // 로컬 호스트 검증
            if (address.isLoopbackAddress()) {
                throw new SsrfValidationException("로컬 호스트 접근이 차단되었습니다: " + address.getHostAddress());
            }

            // 링크 로컬 주소 검증
            if (address.isLinkLocalAddress()) {
                throw new SsrfValidationException("링크 로컬 주소 접근이 차단되었습니다: " + address.getHostAddress());
            }

        } catch (UnknownHostException e) {
            throw new SsrfValidationException("호스트 이름 해석 실패: " + host, e);
        }
    }

    /**
     * 내부 IP 대역 확인
     */
    private boolean isInternalIp(InetAddress address) {
        byte[] ip = address.getAddress();
        
        // IPv4만 처리 (IPv6는 별도 처리 필요시 확장)
        if (ip.length != 4) {
            return false;
        }

        int ipInt = ((ip[0] & 0xFF) << 24) |
                   ((ip[1] & 0xFF) << 16) |
                   ((ip[2] & 0xFF) << 8) |
                   (ip[3] & 0xFF);

        // 내부 IP 대역 확인
        return isInRange(ipInt, "10.0.0.0", 8) ||        // 10.0.0.0/8
               isInRange(ipInt, "172.16.0.0", 12) ||     // 172.16.0.0/12
               isInRange(ipInt, "192.168.0.0", 16) ||    // 192.168.0.0/16
               isInRange(ipInt, "169.254.0.0", 16) ||    // 169.254.0.0/16
               isInRange(ipInt, "127.0.0.0", 8);         // 127.0.0.0/8
    }

    /**
     * IP 주소가 특정 CIDR 범위에 속하는지 확인
     */
    private boolean isInRange(int ip, String cidrNetwork, int prefixLength) {
        try {
            int networkAddress = ipToInt(cidrNetwork);
            int mask = 0xFFFFFFFF << (32 - prefixLength);
            return (ip & mask) == (networkAddress & mask);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * IP 주소 문자열을 정수로 변환
     */
    private int ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        return (Integer.parseInt(parts[0]) << 24) |
               (Integer.parseInt(parts[1]) << 16) |
               (Integer.parseInt(parts[2]) << 8) |
               Integer.parseInt(parts[3]);
    }

    /**
     * 허용된 도메인 목록 반환
     */
    private Set<String> getAllowedDomains() {
        Set<String> domains = new HashSet<>();
        if (StringUtils.hasText(allowedDomainsConfig)) {
            for (String domain : allowedDomainsConfig.split(",")) {
                String trimmed = domain.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    domains.add(trimmed);
                }
            }
        }
        return domains;
    }

    /**
     * 로그용 URL 정제
     */
    private String sanitizeUrlForLog(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        
        // 쿼리 파라미터에서 민감한 정보 마스킹
        return url.replaceAll("([?&](?:token|key|secret|password|auth)=)[^&]*", "$1***")
                 .replaceAll("[\\r\\n\\t]", "_");
    }

    /**
     * SSRF 검증 예외
     */
    public static class SsrfValidationException extends RuntimeException {
        public SsrfValidationException(String message) {
            super(message);
        }
        
        public SsrfValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}