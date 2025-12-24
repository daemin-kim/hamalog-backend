package com.Hamalog.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Hamalog.config.TestRedisConfig;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.auth.request.LoginRequest;
import com.Hamalog.dto.auth.response.LoginResponse;
import com.Hamalog.repository.member.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * E2E 통합 테스트 기반 클래스
 *
 * 실제 애플리케이션 컨텍스트를 로드하여 전체 플로우를 테스트합니다.
 * - 실제 DB (H2 in-memory)
 * - Mock Redis
 * - 실제 Spring Security 필터 체인
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "logging.level.org.hibernate.SQL=warn",
    "hamalog.encryption.key=+ZFRGoRl5CElrJfikdx1TmzQ3U8OJ+J6im5OMjuvsqE=",
    "jwt.secret=test-secret-key-for-jwt-token-generation-must-be-at-least-256-bits-long-for-hs256",
    "jwt.access-token-validity=3600000",
    "jwt.refresh-token-validity=86400000"
})
@Transactional
public abstract class BaseE2ETest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    // 테스트용 회원 정보
    protected static final String TEST_LOGIN_ID = "testuser@hamalog.com";
    protected static final String TEST_PASSWORD = "Test1234!";
    protected static final String TEST_NAME = "테스트유저";
    protected static final String TEST_NICKNAME = "테스트닉";
    protected static final String TEST_PHONE = "01012345678";
    protected static final LocalDate TEST_BIRTH = LocalDate.of(1990, 1, 1);

    protected Member testMember;
    protected String accessToken;
    protected String csrfToken;

    @BeforeEach
    void setUpBase() {
        // 기본 테스트 회원 생성
        testMember = createTestMember(TEST_LOGIN_ID, TEST_PASSWORD, TEST_NAME);
    }

    /**
     * 테스트 회원 생성
     */
    protected Member createTestMember(String loginId, String password, String name) {
        Member member = Member.builder()
            .loginId(loginId)
            .password(passwordEncoder.encode(password))
            .name(name)
            .nickName(TEST_NICKNAME)
            .phoneNumber(TEST_PHONE)
            .birth(TEST_BIRTH)
            .build();
        return memberRepository.save(member);
    }

    /**
     * 로그인하여 토큰 획득
     */
    protected LoginResponse login(String loginId, String password) throws Exception {
        LoginRequest request = new LoginRequest(loginId, password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String csrfHeader = result.getResponse().getHeader("X-CSRF-TOKEN");
        if (csrfHeader != null) {
            this.csrfToken = csrfHeader;
        }

        LoginResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            LoginResponse.class
        );

        this.accessToken = response.token();
        return response;
    }

    /**
     * 기본 테스트 회원으로 로그인
     */
    protected void loginAsTestMember() throws Exception {
        login(TEST_LOGIN_ID, TEST_PASSWORD);
    }

    /**
     * 인증된 요청을 위한 Authorization 헤더 값
     */
    protected String bearerToken() {
        return "Bearer " + accessToken;
    }
}
