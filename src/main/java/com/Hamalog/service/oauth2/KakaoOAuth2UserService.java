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
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"kakao".equalsIgnoreCase(registrationId)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request"),
                    "지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        }

        Map<String, Object> attributes = safeAsMap(oAuth2User.getAttributes(), "root attributes");

        Long kakaoId = safeAsLong(attributes.get("id"), "id")
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"),
                        "카카오 사용자 정보에 'id' 값이 없습니다."));

        Map<String, Object> kakaoAccount = safeAsMap(attributes.get("kakao_account"), "kakao_account");
        Map<String, Object> profile = safeAsMap(kakaoAccount.get("profile"), "profile");

        String nickname = safeAsString(profile.get("nickname")).orElse(null);


        String loginId = "kakao_" + kakaoId + "@oauth2.internal";
        Optional<Member> optionalMember = memberRepository.findByLoginId(loginId);
        if (optionalMember.isEmpty()) {
            String name = (nickname != null && !nickname.isBlank()) ? 
                    (nickname.length() > 15 ? nickname.substring(0, 15) : nickname) : 
                    "OAuth2_" + kakaoId.toString().substring(0, Math.min(kakaoId.toString().length(), 8));
            // Use placeholder phone number for OAuth2 users to meet validation requirements
            String phoneNumber = "01012345678";
            LocalDate birth = LocalDate.of(2000, 1, 1); // Default birth date for OAuth2 users

            String nickName = (nickname != null && !nickname.isBlank()) ? nickname : "카카오유저";
            
            Member member = Member.builder()
                    .loginId(loginId)
                    .password("{oauth2}") // Mark as OAuth2 account, not regular password
                    .name(name)
                    .nickName(nickName)
                    .phoneNumber(phoneNumber)
                    .birth(birth)
                    .createdAt(LocalDateTime.now())
                    .build();
            memberRepository.save(member);
            log.info("Created new OAuth2 member for Kakao id {} as loginId {} - Profile completion required", kakaoId, loginId);
        } else {
            Member existing = optionalMember.get();
            if (nickname != null && !nickname.isBlank() && !nickname.equals(existing.getName())) {
                log.debug("Nickname provided '{}' differs from stored name '{}', but Member is immutable; skipping update.", nickname, existing.getName());
            }
        }

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
