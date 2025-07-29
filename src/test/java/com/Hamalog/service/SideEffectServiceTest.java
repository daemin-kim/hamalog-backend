package com.Hamalog.service;

import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.repository.sideEffect.SideEffectRepository;
import com.Hamalog.service.sideEffect.SideEffectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SideEffectServiceTest {

    private SideEffectRepository sideEffectRepository;
    private SideEffectService sideEffectService;

    @BeforeEach
    void setUp() {
        // 목(repository) 생성
        sideEffectRepository = mock(SideEffectRepository.class);
        sideEffectService = new SideEffectService(sideEffectRepository);
    }

    @Test
    void getRecentSideEffects_정상조회() {
        // given
        Long memberId = 10L;
        List<String> mockNames = List.of("두통", "복통", "현기증");
        when(sideEffectRepository.findRecentSideEffectNames(memberId)).thenReturn(mockNames);

        // when
        RecentSideEffectResponse response = sideEffectService.getRecentSideEffects(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.recentSideEffect()).containsExactly("두통", "복통", "현기증");
        verify(sideEffectRepository).findRecentSideEffectNames(memberId);
    }

    @Test
    void getRecentSideEffects_빈결과_테스트() {
        // given
        Long memberId = 3000L; // 없을 수 있는 유저
        when(sideEffectRepository.findRecentSideEffectNames(memberId)).thenReturn(List.of());

        // when
        RecentSideEffectResponse response = sideEffectService.getRecentSideEffects(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.recentSideEffect()).isEmpty();
        verify(sideEffectRepository).findRecentSideEffectNames(memberId);
    }
}
