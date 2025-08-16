package com.Hamalog.controller.sideEffect;

import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.service.sideEffect.SideEffectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(SideEffectController.class)
@DisplayName("SideEffect Controller Unit Tests")
class SideEffectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SideEffectService sideEffectService;

    @Test
    @DisplayName("최근 부작용 목록 조회 성공")
    @WithMockUser(username = "testuser")
    void getRecentSideEffects_Success() throws Exception {
        // Given
        Long userId = 1L;
        List<String> recentSideEffects = Arrays.asList("두통", "메스꺼움", "어지러움");
        RecentSideEffectResponse response = new RecentSideEffectResponse(recentSideEffects);
        
        given(sideEffectService.isOwner(userId, "testuser"))
                .willReturn(true);
        given(sideEffectService.getRecentSideEffects(userId))
                .willReturn(response);

        // When & Then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", userId.toString())
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentSideEffect").isArray())
                .andExpect(jsonPath("$.recentSideEffect.length()").value(3))
                .andExpect(jsonPath("$.recentSideEffect[0]").value("두통"))
                .andExpect(jsonPath("$.recentSideEffect[1]").value("메스꺼움"))
                .andExpect(jsonPath("$.recentSideEffect[2]").value("어지러움"));

        verify(sideEffectService).isOwner(userId, "testuser");
        verify(sideEffectService).getRecentSideEffects(userId);
    }

    @Test
    @DisplayName("최근 부작용 목록 조회 성공 - 빈 목록")
    @WithMockUser(username = "testuser")
    void getRecentSideEffects_SuccessEmptyList() throws Exception {
        // Given
        Long userId = 1L;
        List<String> emptyList = Arrays.asList();
        RecentSideEffectResponse response = new RecentSideEffectResponse(emptyList);
        
        given(sideEffectService.isOwner(userId, "testuser"))
                .willReturn(true);
        given(sideEffectService.getRecentSideEffects(userId))
                .willReturn(response);

        // When & Then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", userId.toString())
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentSideEffect").isArray())
                .andExpect(jsonPath("$.recentSideEffect.length()").value(0));

        verify(sideEffectService).isOwner(userId, "testuser");
        verify(sideEffectService).getRecentSideEffects(userId);
    }

    @Test
    @DisplayName("최근 부작용 목록 조회 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void getRecentSideEffects_Forbidden() throws Exception {
        // Given
        Long userId = 1L;
        
        given(sideEffectService.isOwner(userId, "testuser"))
                .willReturn(false);

        // When & Then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", userId.toString())
                .with(user("testuser")))
                .andExpect(status().isForbidden());

        verify(sideEffectService).isOwner(userId, "testuser");
    }

    @Test
    @DisplayName("최근 부작용 목록 조회 실패 - 사용자 찾을 수 없음")
    @WithMockUser(username = "testuser")
    void getRecentSideEffects_UserNotFound() throws Exception {
        // Given
        Long nonExistentUserId = 999L;
        
        given(sideEffectService.isOwner(nonExistentUserId, "testuser"))
                .willReturn(true);
        willThrow(new RuntimeException("사용자를 찾을 수 없습니다"))
                .given(sideEffectService).getRecentSideEffects(nonExistentUserId);

        // When & Then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", nonExistentUserId.toString())
                .with(user("testuser")))
                .andExpect(status().is5xxServerError());

        verify(sideEffectService).isOwner(nonExistentUserId, "testuser");
        verify(sideEffectService).getRecentSideEffects(nonExistentUserId);
    }

    @Test
    @DisplayName("최근 부작용 목록 조회 실패 - 필수 파라미터 누락")
    @WithMockUser(username = "testuser")
    void getRecentSideEffects_MissingParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/side-effect/recent")
                .with(user("testuser")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("최근 부작용 목록 조회 실패 - 잘못된 파라미터 타입")
    @WithMockUser(username = "testuser")
    void getRecentSideEffects_InvalidParameterType() throws Exception {
        // When & Then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", "invalid-user-id")
                .with(user("testuser")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("최근 부작용 목록 조회 성공 - 5개 제한 확인")
    @WithMockUser(username = "testuser")
    void getRecentSideEffects_SuccessFiveItems() throws Exception {
        // Given
        Long userId = 1L;
        List<String> fiveSideEffects = Arrays.asList("두통", "메스꺼움", "어지러움", "복통", "피로감");
        RecentSideEffectResponse response = new RecentSideEffectResponse(fiveSideEffects);
        
        given(sideEffectService.isOwner(userId, "testuser"))
                .willReturn(true);
        given(sideEffectService.getRecentSideEffects(userId))
                .willReturn(response);

        // When & Then
        mockMvc.perform(get("/side-effect/recent")
                .param("userId", userId.toString())
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentSideEffect").isArray())
                .andExpect(jsonPath("$.recentSideEffect.length()").value(5))
                .andExpect(jsonPath("$.recentSideEffect[0]").value("두통"))
                .andExpect(jsonPath("$.recentSideEffect[1]").value("메스꺼움"))
                .andExpect(jsonPath("$.recentSideEffect[2]").value("어지러움"))
                .andExpect(jsonPath("$.recentSideEffect[3]").value("복통"))
                .andExpect(jsonPath("$.recentSideEffect[4]").value("피로감"));

        verify(sideEffectService).isOwner(userId, "testuser");
        verify(sideEffectService).getRecentSideEffects(userId);
    }
}