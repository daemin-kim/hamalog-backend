package com.Hamalog.security.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class TrustedProxyService {

    private final List<IpAddressMatcher> trustedProxyMatchers;
    private final List<String> trustedProxyCidrs;

    public TrustedProxyService(
            @Value("${hamalog.security.trusted-proxies:127.0.0.1/32,::1/128}") String trustedProxiesProperty
    ) {
        this.trustedProxyCidrs = parseProperty(trustedProxiesProperty);
        this.trustedProxyMatchers = trustedProxyCidrs.stream()
                .map(this::createMatcher)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public boolean isTrustedProxy(String remoteAddress) {
        if (!StringUtils.hasText(remoteAddress) || trustedProxyMatchers.isEmpty()) {
            return false;
        }
        return trustedProxyMatchers.stream().anyMatch(matcher -> matcher.matches(remoteAddress));
    }

    public Optional<String> extractClientIp(String xForwardedForHeader) {
        if (!StringUtils.hasText(xForwardedForHeader)) {
            return Optional.empty();
        }

        return Stream.of(xForwardedForHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(candidate -> !"unknown".equalsIgnoreCase(candidate))
                .filter(this::isValidIpLiteral)
                .findFirst();
    }

    public List<String> getTrustedProxyCidrs() {
        return Collections.unmodifiableList(trustedProxyCidrs);
    }

    private List<String> parseProperty(String property) {
        if (!StringUtils.hasText(property)) {
            return List.of();
        }

        return Stream.of(property.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private IpAddressMatcher createMatcher(String cidr) {
        try {
            return new IpAddressMatcher(cidr);
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring invalid trusted proxy CIDR: {}", cidr, ex);
            return null;
        }
    }

    private boolean isValidIpLiteral(String candidate) {
        // Reject anything that contains non-IP characters to avoid DNS lookups
        if (!candidate.matches("^[0-9a-fA-F:.,]+$")) {
            return false;
        }
        try {
            InetAddress.getByName(candidate);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
