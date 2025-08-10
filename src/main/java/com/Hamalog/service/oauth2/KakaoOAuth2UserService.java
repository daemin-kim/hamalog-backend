package com.Hamalog.service.oauth2;

import com.Hamalog.domain.member.Member;
import com.Hamalog.repository.member.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuth2UserService.class);

    private final MemberRepository memberRepository;

    public KakaoOAuth2UserService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1) Delegate to default loader
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2) Verify provider (defensive)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"kakao".equalsIgnoreCase(registrationId)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request"),
                    "Unsupported OAuth2 provider: " + registrationId);
        }

        // 3) Safely extract attributes
        Map<String, Object> attributes = safeAsMap(oAuth2User.getAttributes(), "root attributes");

        Long kakaoId = safeAsLong(attributes.get("id"), "id")
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"),
                        "Kakao user info missing 'id'"));

        Map<String, Object> kakaoAccount = safeAsMap(attributes.get("kakao_account"), "kakao_account");
        Map<String, Object> profile = safeAsMap(kakaoAccount.get("profile"), "profile");

        String nickname = safeAsString(profile.get("nickname")).orElse(null);

        // Optional: check agreement flags when present
        // Boolean hasEmail = safeAsBoolean(kakaoAccount.get("has_email")).orElse(null);
        // String email = safeAsString(kakaoAccount.get("email")).orElse(null);

        // 4) Persist or update local member
        String loginId = "kakao_" + kakaoId; // ex: kakao_12345678
        Optional<Member> optionalMember = memberRepository.findByLoginId(loginId);
        if (optionalMember.isEmpty()) {
            String name = (nickname != null && !nickname.isBlank()) ? nickname : "카카오사용자";
            String phoneNumber = "0000000000000"; // 13 digits placeholder
            LocalDate birth = LocalDate.of(1970, 1, 1); // default placeholder

            Member member = Member.builder()
                    .loginId(loginId)
                    .password("{noop}") // Social login placeholder; not used
                    .name(name)
                    .phoneNumber(phoneNumber)
                    .birth(birth)
                    .createdAt(LocalDateTime.now())
                    .build();
            memberRepository.save(member);
            log.debug("Created new member for Kakao id {} as loginId {}", kakaoId, loginId);
        } else {
            // Optionally update simple profile fields if present
            Member existing = optionalMember.get();
            if (nickname != null && !nickname.isBlank() && !nickname.equals(existing.getName())) {
                // Since Member fields are immutable (no setters), skip update for now to keep minimal changes
                log.debug("Nickname provided '{}' differs from stored name '{}', but Member is immutable; skipping update.", nickname, existing.getName());
            }
        }

        // 5) Return the delegate-provided user (authorities handled by framework)
        return oAuth2User;
    }

    private Map<String, Object> safeAsMap(Object obj, String fieldName) {
        if (obj instanceof Map<?, ?> map) {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                Object k = entry.getKey();
                if (k instanceof String key) {
                    result.put(key, entry.getValue());
                }
            }
            return result;
        }
        if (obj == null) {
            return Map.of();
        }
        log.debug("Expected '{}' to be a Map but was {} — treating as empty.", fieldName, obj.getClass().getName());
        return Map.of();
    }

    private Optional<String> safeAsString(Object obj) {
        if (obj == null) return Optional.empty();
        if (obj instanceof String s) return Optional.of(s);
        return Optional.ofNullable(String.valueOf(obj));
    }

    private Optional<Long> safeAsLong(Object obj, String fieldName) {
        if (obj == null) return Optional.empty();
        if (obj instanceof Number n) return Optional.of(n.longValue());
        if (obj instanceof String s) {
            try {
                return Optional.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                log.warn("Field '{}' expected to be numeric but was string '{}'", fieldName, s);
                return Optional.empty();
            }
        }
        log.warn("Field '{}' expected to be numeric but was {}", fieldName, obj.getClass().getName());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private Optional<Boolean> safeAsBoolean(Object obj) {
        if (obj == null) return Optional.empty();
        if (obj instanceof Boolean b) return Optional.of(b);
        if (obj instanceof String s) return Optional.of(Boolean.parseBoolean(s));
        return Optional.empty();
    }
}
