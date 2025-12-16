package com.Hamalog.controller.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Hamalog.dto.diary.request.MoodDiaryCreateRequest;
import com.Hamalog.dto.diary.response.MoodDiaryListResponse;
import com.Hamalog.dto.diary.response.MoodDiaryResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.handler.GlobalExceptionHandler;
import com.Hamalog.security.CustomUserDetails;
import com.Hamalog.security.filter.TrustedProxyService;
import com.Hamalog.service.diary.MoodDiaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoodDiaryController 테스트")
class MoodDiaryControllerTest {

    @Mock
    private MoodDiaryService moodDiaryService;

    @Mock
    private TrustedProxyService trustedProxyService;

    @InjectMocks
    private MoodDiaryController moodDiaryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(moodDiaryController)
                .setCustomArgumentResolvers(new CustomUserResolver())
                .setControllerAdvice(new GlobalExceptionHandler(trustedProxyService))
                .defaultResponseCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8)
                .build();

        userDetails = buildUserDetails(1L, "user@example.com");
    }

    private CustomUserDetails buildUserDetails(Long id, String loginId) {
        return new CustomUserDetails(com.Hamalog.domain.member.Member.builder()
                .memberId(id)
                .loginId(loginId)
                .password("encoded")
                .name("사용자")
                .phoneNumber("01012345678")
                .nickName("테스터")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private class CustomUserResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(CustomUserDetails.class) &&
                    parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            Object principal = webRequest.getAttribute("__TEST_PRINCIPAL__", NativeWebRequest.SCOPE_REQUEST);
            if (principal == null) {
                throw new CustomException(ErrorCode.UNAUTHORIZED) {};
            }
            return principal;
        }
    }

    @Test
    @DisplayName("마음 일기 생성 성공")
    void createMoodDiary_Success() throws Exception {
        MoodDiaryCreateRequest request = createTemplateRequest();
        MoodDiaryResponse response = MoodDiaryResponse.builder()
                .moodDiaryId(10L)
                .memberId(1L)
                .diaryDate(request.getDiaryDate())
                .diaryType(request.getDiaryType())
                .templateAnswer1(request.getTemplateAnswer1())
                .templateAnswer2(request.getTemplateAnswer2())
                .templateAnswer3(request.getTemplateAnswer3())
                .templateAnswer4(request.getTemplateAnswer4())
                .build();

        when(moodDiaryService.createMoodDiary(eq(1L), any(MoodDiaryCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/mood-diary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("__TEST_PRINCIPAL__", userDetails))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moodDiaryId").value(10L));

        ArgumentCaptor<MoodDiaryCreateRequest> captor = ArgumentCaptor.forClass(MoodDiaryCreateRequest.class);
        verify(moodDiaryService).createMoodDiary(eq(1L), captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("인증 사용자와 요청 회원 ID 불일치 시 403")
    void createMoodDiary_ForbiddenWhenMemberMismatch() throws Exception {
        MoodDiaryCreateRequest request = MoodDiaryCreateRequest.builder()
                .memberId(2L)
                .diaryDate(LocalDate.of(2025, 12, 1))
                .moodType(com.Hamalog.domain.diary.MoodType.HAPPY)
                .diaryType(com.Hamalog.domain.diary.DiaryType.TEMPLATE)
                .templateAnswer1("A1")
                .templateAnswer2("A2")
                .templateAnswer3("A3")
                .templateAnswer4("A4")
                .build();

        mockMvc.perform(post("/mood-diary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("__TEST_PRINCIPAL__", userDetails))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moodDiaryService);
    }

    @Test
    @DisplayName("마음 일기 단건 조회 성공")
    void getMoodDiary_Success() throws Exception {
        MoodDiaryResponse response = MoodDiaryResponse.builder()
                .moodDiaryId(5L)
                .memberId(1L)
                .diaryDate(LocalDate.of(2025, 12, 1))
                .build();

        when(moodDiaryService.getMoodDiary(5L, 1L)).thenReturn(response);

        mockMvc.perform(get("/mood-diary/{mood-diary-id}", 5L)
                        .requestAttr("__TEST_PRINCIPAL__", userDetails))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moodDiaryId").value(5L));
    }

    @Test
    @DisplayName("마음 일기 단건 조회 실패")
    void getMoodDiary_NotFound() throws Exception {
        when(moodDiaryService.getMoodDiary(5L, 1L)).thenThrow(new CustomException(ErrorCode.MOOD_DIARY_NOT_FOUND) {});

        mockMvc.perform(get("/mood-diary/{mood-diary-id}", 5L)
                        .requestAttr("__TEST_PRINCIPAL__", userDetails))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("회원 일기 목록 조회 - 페이지 크기 제한 확인")
    void getMoodDiariesByMember_SizeCapped() throws Exception {
        MoodDiaryListResponse response = MoodDiaryListResponse.builder()
                .diaries(List.of())
                .currentPage(0)
                .pageSize(100)
                .totalCount(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(moodDiaryService.getMoodDiariesByMember(1L, 0, 200)).thenReturn(response);

        mockMvc.perform(get("/mood-diary/list/{member-id}", 1L)
                        .param("page", "0")
                        .param("size", "200")
                        .requestAttr("__TEST_PRINCIPAL__", userDetails))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(100));
    }

    @Test
    @DisplayName("특정 날짜 일기 조회 성공")
    void getMoodDiaryByDate_Success() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 3);
        MoodDiaryResponse response = MoodDiaryResponse.builder()
                .moodDiaryId(7L)
                .memberId(1L)
                .diaryDate(date)
                .build();

        when(moodDiaryService.getMoodDiaryByDate(1L, date)).thenReturn(response);

        mockMvc.perform(get("/mood-diary/date/{member-id}", 1L)
                        .param("diaryDate", date.toString())
                        .requestAttr("__TEST_PRINCIPAL__", userDetails))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moodDiaryId").value(7L));
    }

    @Test
    @DisplayName("마음 일기 삭제 성공")
    void deleteMoodDiary_Success() throws Exception {
        doNothing().when(moodDiaryService).deleteMoodDiary(9L, 1L);

        mockMvc.perform(delete("/mood-diary/{mood-diary-id}", 9L)
                        .requestAttr("__TEST_PRINCIPAL__", userDetails))
                .andExpect(status().isNoContent());

        verify(moodDiaryService).deleteMoodDiary(9L, 1L);
    }

    @Test
    @DisplayName("회원 일기 목록 조회 권한 실패")
    void getMoodDiariesByMember_Forbidden() throws Exception {
        mockMvc.perform(get("/mood-diary/list/{member-id}", 2L)
                        .param("page", "0")
                        .param("size", "20")
                        .requestAttr("__TEST_PRINCIPAL__", userDetails))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moodDiaryService);
    }

    @Test
    @DisplayName("특정 날짜 조회 권한 실패")
    void getMoodDiaryByDate_Forbidden() throws Exception {
        mockMvc.perform(get("/mood-diary/date/{member-id}", 2L)
                        .param("diaryDate", LocalDate.now().toString())
                        .requestAttr("__TEST_PRINCIPAL__", userDetails))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moodDiaryService);
    }

    @Test
    @DisplayName("마음 일기 삭제 권한 실패")
    void deleteMoodDiary_Forbidden() {
        doThrow(new CustomException(ErrorCode.FORBIDDEN) {})
                .when(moodDiaryService).deleteMoodDiary(5L, 1L);

        assertThatThrownBy(() -> moodDiaryController.deleteMoodDiary(5L, userDetails))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private MoodDiaryCreateRequest createTemplateRequest() {
        return MoodDiaryCreateRequest.builder()
                .memberId(1L)
                .diaryDate(LocalDate.of(2025, 12, 1))
                .moodType(com.Hamalog.domain.diary.MoodType.HAPPY)
                .diaryType(com.Hamalog.domain.diary.DiaryType.TEMPLATE)
                .templateAnswer1("A1")
                .templateAnswer2("A2")
                .templateAnswer3("A3")
                .templateAnswer4("A4")
                .build();
    }
}
