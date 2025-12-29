package com.Hamalog.controller.sideEffect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Hamalog.dto.sideEffect.request.SideEffectRecordRequest;
import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.service.sideEffect.SideEffectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Side Effect Controller Tests")
class SideEffectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SideEffectService sideEffectService;

    @InjectMocks
    private SideEffectController sideEffectController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sideEffectController)
                .setCustomArgumentResolvers(new MockUserDetailsArgumentResolver())
                .setHandlerExceptionResolvers(new SimpleExceptionResolver())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register JavaTimeModule for LocalDateTime
    }

    /**
     * Custom argument resolver for @AuthenticationPrincipal UserDetails parameters
     */
    private static class MockUserDetailsArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class) &&
                   UserDetails.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            UserDetails mockUserDetails = mock(UserDetails.class);
            lenient().when(mockUserDetails.getUsername()).thenReturn("testuser");
            return mockUserDetails;
        }
    }

    /**
     * Simple exception resolver for handling service exceptions
     */
    private static class SimpleExceptionResolver implements HandlerExceptionResolver {

        @Override
        public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                           Object handler, Exception ex) {
            if (ex.getClass().getSimpleName().contains("MethodArgumentNotValid") ||
                ex.getClass().getSimpleName().contains("TypeMismatch") ||
                ex.getClass().getSimpleName().contains("HttpMessageNotReadable") ||
                ex.getClass().getSimpleName().contains("MissingServletRequest") ||
                ex.getClass().getSimpleName().contains("ConstraintViolation") ||
                ex.getClass().getSimpleName().contains("HttpMediaTypeNotSupported")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return new ModelAndView();
            }
            else if (ex instanceof RuntimeException) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new ModelAndView();
            }
            return null;
        }
    }

    @Test
    @DisplayName("Should get recent side effects successfully")
    void getRecentSideEffects_ValidUserId_ShouldReturnSideEffects() throws Exception {
        // given
        Long userId = 1L;
        List<String> sideEffects = Arrays.asList("두통", "메스꺼움", "어지럼증");
        RecentSideEffectResponse response = new RecentSideEffectResponse(sideEffects);

        when(sideEffectService.getRecentSideEffects(userId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.recentSideEffect.length()").value(3))
                .andExpect(jsonPath("$.recentSideEffect[0]").value("두통"))
                .andExpect(jsonPath("$.recentSideEffect[1]").value("메스꺼움"))
                .andExpect(jsonPath("$.recentSideEffect[2]").value("어지럼증"));

        verify(sideEffectService).getRecentSideEffects(userId);
    }

    @Test
    @DisplayName("Should return empty list when no recent side effects found")
    void getRecentSideEffects_NoSideEffects_ShouldReturnEmptyList() throws Exception {
        // given
        Long userId = 1L;
        RecentSideEffectResponse response = new RecentSideEffectResponse(Collections.emptyList());

        when(sideEffectService.getRecentSideEffects(userId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentSideEffect.length()").value(0));

        verify(sideEffectService).getRecentSideEffects(userId);
    }

    @Test
    @DisplayName("Should return 400 when userId parameter is missing")
    void getRecentSideEffects_MissingUserId_ShouldReturn400() throws Exception {
        // when & then
        mockMvc.perform(get("/side-effect/recent"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sideEffectService);
    }

    @Test
    @DisplayName("Should return 400 when userId parameter is invalid")
    void getRecentSideEffects_InvalidUserId_ShouldReturn400() throws Exception {
        // when & then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", "invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sideEffectService);
    }

    @Test
    @DisplayName("Should handle service exception properly")
    void getRecentSideEffects_ServiceException_ShouldReturn500() throws Exception {
        // given
        Long userId = 1L;
        when(sideEffectService.getRecentSideEffects(userId))
                .thenThrow(new RuntimeException("Service error"));

        // when & then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", userId.toString()))
                .andExpect(status().isInternalServerError());

        verify(sideEffectService).getRecentSideEffects(userId);
    }

    @Test
    @DisplayName("Should create side effect record successfully")
    void createSideEffectRecord_ValidRequest_ShouldReturnCreated() throws Exception {
        // given
        SideEffectRecordRequest request = createTestSideEffectRequest();
        doNothing().when(sideEffectService).createSideEffectRecord(any(SideEffectRecordRequest.class));

        // when & then
        mockMvc.perform(post("/side-effect/record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(sideEffectService).createSideEffectRecord(any(SideEffectRecordRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for malformed JSON in create request")
    void createSideEffectRecord_MalformedJson_ShouldReturn400() throws Exception {
        // given
        String malformedJson = "{\"invalid\": " + "json" + "}";

        // when & then
        mockMvc.perform(post("/side-effect/record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sideEffectService);
    }

    @Test
    @DisplayName("Should return 400 when request body is missing")
    void createSideEffectRecord_MissingRequestBody_ShouldReturn400() throws Exception {
        // when & then
        mockMvc.perform(post("/side-effect/record")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sideEffectService);
    }

    @Test
    @DisplayName("Should handle service exception during creation")
    void createSideEffectRecord_ServiceException_ShouldReturn500() throws Exception {
        // given
        SideEffectRecordRequest request = createTestSideEffectRequest();
        doThrow(new RuntimeException("Creation failed"))
                .when(sideEffectService).createSideEffectRecord(any(SideEffectRecordRequest.class));

        // when & then
        mockMvc.perform(post("/side-effect/record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(sideEffectService).createSideEffectRecord(any(SideEffectRecordRequest.class));
    }

    @Test
    @DisplayName("Should handle different content types properly")
    void createSideEffectRecord_WrongContentType_ShouldReturn400() throws Exception {
        // given
        SideEffectRecordRequest request = createTestSideEffectRequest();

        // when & then
        mockMvc.perform(post("/side-effect/record")
                .contentType(MediaType.TEXT_PLAIN)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sideEffectService);
    }

    @Test
    @DisplayName("Should handle null userId parameter")
    void getRecentSideEffects_NullUserId_ShouldReturn400() throws Exception {
        // when & then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", ""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sideEffectService);
    }

    @Test
    @DisplayName("Should handle zero userId parameter")
    void getRecentSideEffects_ZeroUserId_ShouldCallService() throws Exception {
        // given
        Long userId = 0L;
        RecentSideEffectResponse response = new RecentSideEffectResponse(Collections.emptyList());

        when(sideEffectService.getRecentSideEffects(userId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", userId.toString()))
                .andExpect(status().isOk());

        verify(sideEffectService).getRecentSideEffects(userId);
    }

    @Test
    @DisplayName("Should handle negative userId parameter")
    void getRecentSideEffects_NegativeUserId_ShouldCallService() throws Exception {
        // given
        Long userId = -1L;
        RecentSideEffectResponse response = new RecentSideEffectResponse(Collections.emptyList());

        when(sideEffectService.getRecentSideEffects(userId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", userId.toString()))
                .andExpect(status().isOk());

        verify(sideEffectService).getRecentSideEffects(userId);
    }

    // Helper methods
    private SideEffectRecordRequest createTestSideEffectRequest() {
        // Create SideEffectItem objects
        SideEffectRecordRequest.SideEffectItem item1 = new SideEffectRecordRequest.SideEffectItem(1L, 2);
        SideEffectRecordRequest.SideEffectItem item2 = new SideEffectRecordRequest.SideEffectItem(2L, 3);

        return new SideEffectRecordRequest(
                1L, // memberId
                LocalDateTime.now(), // recordedAt
                null, // linkedMedicationScheduleId
                Arrays.asList(item1, item2) // sideEffects
        );
    }
}