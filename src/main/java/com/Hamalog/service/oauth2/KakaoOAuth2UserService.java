package com.Hamalog.service.oauth2;

import com.Hamalog.domain.member.Member;
import com.Hamalog.repository.member.MemberRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    public KakaoOAuth2UserService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        long kakaoId = ((Number) attributes.get("id")).longValue();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String loginId = "kakao_" + kakaoId; // ex: kakao_12345678
        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        Optional<Member> optionalMember = memberRepository.findByLoginId(loginId);
        Member member;
        if (optionalMember.isPresent()) {
            member = optionalMember.get();
            // 필요하다면 nickname, email 등 정보 업데이트
        } else {
            member = Member.builder()
                    .loginId(loginId)
                    .password("") // 소셜 로그인은 비밀번호 없음
                    .phoneNumber("") // 필요시 저장
                    .birth(null) // 필요시 저장
                    .createdAt(LocalDateTime.now())
                    .build();
            memberRepository.save(member);
        }

        // 필요시 커스텀 OAuth2User 반환 (권한, 추가정보 포함)
        return oAuth2User;
    }
}
