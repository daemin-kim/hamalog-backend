package com.Hamalog.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.Hamalog.security.csrf.CsrfTokenProvider;
import com.Hamalog.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CsrfValidationFilter Tests")
class CsrfValidationFilterTest {

    @Mock
    private CsrfTokenProvider csrfTokenProvider;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private PrintWriter printWriter;

    private CsrfValidationFilter filter;
    private StringWriter stringWriter;

    @BeforeEach
    void setUp() throws IOException {
        filter = new CsrfValidationFilter(csrfTokenProvider, jwtTokenProvider);
        stringWriter = new StringWriter();
        
        // Use lenient stubbing to avoid unnecessary stubbing exceptions
        lenient().when(response.getWriter()).thenReturn(printWriter);
        lenient().doAnswer(invocation -> {
            String text = invocation.getArgument(0);
            stringWriter.write(text);
            return null;
        }).when(printWriter).write(anyString());
    }

    @Test
    @DisplayName("GET 요청은 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_GetRequest_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("HEAD 요청은 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_HeadRequest_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("HEAD");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("OPTIONS 요청은 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_OptionsRequest_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("OPTIONS");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 경로는 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_LoginPath_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/auth/login");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("회원가입 경로는 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_SignupPath_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/auth/signup");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("CSRF 토큰 발급 경로는 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_CsrfTokenPath_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/auth/csrf-token");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("OAuth2 경로는 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_OAuth2Path_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/oauth2/authorization/kakao");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Swagger 경로는 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_SwaggerPath_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/v3/api-docs");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Actuator 경로는 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_ActuatorPath_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/actuator/health");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("JWT 토큰이 없는 POST 요청은 403 에러를 반환해야 함")
    void doFilterInternal_PostWithoutJwt_ShouldReturn403() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn(null);
        

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(filterChain, never()).doFilter(request, response);
        
        ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).write(responseCaptor.capture());
        
        String responseJson = responseCaptor.getValue();
        assertThat(responseJson).contains("\"error\":\"CSRF_TOKEN_INVALID\"");
        assertThat(responseJson).contains("\"message\":\"인증이 필요합니다\"");
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰이 있는 POST 요청은 403 에러를 반환해야 함")
    void doFilterInternal_PostWithInvalidJwt_ShouldReturn403() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn("Bearer invalid-token");
        given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);
        
        // Setup response writer for error response
        given(response.getWriter()).willReturn(printWriter);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 Authorization 헤더는 무효해야 함")
    void doFilterInternal_AuthHeaderWithoutBearer_ShouldReturn403() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn("Token some-token");
        
        // Setup response writer for error response
        given(response.getWriter()).willReturn(printWriter);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 토큰 추출 중 예외가 발생하면 403 에러를 반환해야 함")
    void doFilterInternal_JwtExtractionException_ShouldReturn403() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn("Bearer valid-token");
        given(jwtTokenProvider.validateToken("valid-token")).willThrow(new RuntimeException("JWT parsing error"));
        
        // Setup response writer for error response
        given(response.getWriter()).willReturn(printWriter);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("유효한 JWT가 있지만 CSRF 토큰이 없는 경우 403 에러를 반환해야 함")
    void doFilterInternal_ValidJwtButNoCsrfToken_ShouldReturn403() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn("Bearer valid-jwt-token");
        given(request.getHeader("X-CSRF-TOKEN")).willReturn(null);
        given(jwtTokenProvider.validateToken("valid-jwt-token")).willReturn(true);
        given(jwtTokenProvider.getLoginIdFromToken("valid-jwt-token")).willReturn("user123");
        given(csrfTokenProvider.validateToken("user123", null)).willReturn(false);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
        
        ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).write(responseCaptor.capture());
        
        String responseJson = responseCaptor.getValue();
        assertThat(responseJson).contains("\"message\":\"CSRF 토큰이 유효하지 않습니다\"");
    }

    @Test
    @DisplayName("유효하지 않은 CSRF 토큰이 있는 경우 403 에러를 반환해야 함")
    void doFilterInternal_InvalidCsrfToken_ShouldReturn403() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn("Bearer valid-jwt-token");
        given(request.getHeader("X-CSRF-TOKEN")).willReturn("invalid-csrf-token");
        given(jwtTokenProvider.validateToken("valid-jwt-token")).willReturn(true);
        given(jwtTokenProvider.getLoginIdFromToken("valid-jwt-token")).willReturn("user123");
        given(csrfTokenProvider.validateToken("user123", "invalid-csrf-token")).willReturn(false);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("유효한 JWT와 CSRF 토큰이 있는 POST 요청은 성공해야 함")
    void doFilterInternal_ValidJwtAndCsrfToken_ShouldSucceed() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn("Bearer valid-jwt-token");
        given(request.getHeader("X-CSRF-TOKEN")).willReturn("valid-csrf-token");
        given(jwtTokenProvider.validateToken("valid-jwt-token")).willReturn(true);
        given(jwtTokenProvider.getLoginIdFromToken("valid-jwt-token")).willReturn("user123");
        given(csrfTokenProvider.validateToken("user123", "valid-csrf-token")).willReturn(true);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("PUT 요청도 CSRF 검증이 적용되어야 함")
    void doFilterInternal_PutRequest_ShouldRequireCsrfValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("PUT");
        given(request.getRequestURI()).willReturn("/api/medication/schedules/1");
        given(request.getHeader("Authorization")).willReturn("Bearer valid-jwt-token");
        given(request.getHeader("X-CSRF-TOKEN")).willReturn("valid-csrf-token");
        given(jwtTokenProvider.validateToken("valid-jwt-token")).willReturn(true);
        given(jwtTokenProvider.getLoginIdFromToken("valid-jwt-token")).willReturn("user123");
        given(csrfTokenProvider.validateToken("user123", "valid-csrf-token")).willReturn(true);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider).validateToken("user123", "valid-csrf-token");
    }

    @Test
    @DisplayName("PATCH 요청도 CSRF 검증이 적용되어야 함")
    void doFilterInternal_PatchRequest_ShouldRequireCsrfValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("PATCH");
        given(request.getRequestURI()).willReturn("/api/medication/schedules/1");
        given(request.getHeader("Authorization")).willReturn("Bearer valid-jwt-token");
        given(request.getHeader("X-CSRF-TOKEN")).willReturn("valid-csrf-token");
        given(jwtTokenProvider.validateToken("valid-jwt-token")).willReturn(true);
        given(jwtTokenProvider.getLoginIdFromToken("valid-jwt-token")).willReturn("user123");
        given(csrfTokenProvider.validateToken("user123", "valid-csrf-token")).willReturn(true);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider).validateToken("user123", "valid-csrf-token");
    }

    @Test
    @DisplayName("DELETE 요청도 CSRF 검증이 적용되어야 함")
    void doFilterInternal_DeleteRequest_ShouldRequireCsrfValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("DELETE");
        given(request.getRequestURI()).willReturn("/api/medication/schedules/1");
        given(request.getHeader("Authorization")).willReturn("Bearer valid-jwt-token");
        given(request.getHeader("X-CSRF-TOKEN")).willReturn("valid-csrf-token");
        given(jwtTokenProvider.validateToken("valid-jwt-token")).willReturn(true);
        given(jwtTokenProvider.getLoginIdFromToken("valid-jwt-token")).willReturn("user123");
        given(csrfTokenProvider.validateToken("user123", "valid-csrf-token")).willReturn(true);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider).validateToken("user123", "valid-csrf-token");
    }

    @Test
    @DisplayName("에러 응답에 타임스탬프가 포함되어야 함")
    void doFilterInternal_ErrorResponse_ShouldIncludeTimestamp() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
        verify(printWriter).write(responseCaptor.capture());
        
        String responseJson = responseCaptor.getValue();
        assertThat(responseJson).contains("\"timestamp\":");
        assertThat(responseJson).contains("\"error\":\"CSRF_TOKEN_INVALID\"");
    }

    @Test
    @DisplayName("빈 Authorization 헤더는 무효해야 함")
    void doFilterInternal_EmptyAuthHeader_ShouldReturn403() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn("");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("공백만 있는 Authorization 헤더는 무효해야 함")
    void doFilterInternal_WhitespaceAuthHeader_ShouldReturn403() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn("   ");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 토큰은 있지만 사용자 ID를 추출할 수 없는 경우 403 에러를 반환해야 함")
    void doFilterInternal_JwtValidButNoUserId_ShouldReturn403() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/medication/schedules");
        given(request.getHeader("Authorization")).willReturn("Bearer valid-jwt-token");
        given(jwtTokenProvider.validateToken("valid-jwt-token")).willReturn(true);
        given(jwtTokenProvider.getLoginIdFromToken("valid-jwt-token")).willReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("에러 경로는 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_ErrorPath_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/error");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Swagger UI 경로는 CSRF 검증을 건너뛰어야 함")
    void doFilterInternal_SwaggerUiPath_ShouldSkipValidation() throws ServletException, IOException {
        // given
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/swagger-ui/index.html");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(csrfTokenProvider, never()).validateToken(anyString(), anyString());
    }
}