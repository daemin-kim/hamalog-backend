package com.Hamalog.service.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("State Persistence Service Tests")
class StatePersistenceServiceTest {

    private StatePersistenceService service;

    @BeforeEach
    void setUp() {
        service = new StatePersistenceService(null);  // Redis null = 메모리 모드
    }

    @Test
    @DisplayName("State 생성 및 저장 성공")
    void testGenerateAndStoreState() {
        String state = service.generateAndStoreState();

        assertNotNull(state);
        assertFalse(state.isEmpty());
        assertEquals(36, state.length());  // UUID 길이
    }

    @Test
    @DisplayName("State 검증 성공")
    void testValidateAndConsumeStateSuccess() {
        String state = service.generateAndStoreState();

        assertTrue(service.validateAndConsumeState(state));
    }

    @Test
    @DisplayName("State 일회용 검증 - 재사용 불가")
    void testStateCanOnlyBeUsedOnce() {
        String state = service.generateAndStoreState();

        assertTrue(service.validateAndConsumeState(state));
        assertFalse(service.validateAndConsumeState(state));  // 두 번째 사용 실패
    }

    @Test
    @DisplayName("존재하지 않는 State 검증 실패")
    void testValidateNonExistentState() {
        assertFalse(service.validateAndConsumeState("non-existent-state"));
    }

    @Test
    @DisplayName("Null State 검증 실패")
    void testValidateNullState() {
        assertFalse(service.validateAndConsumeState(null));
    }

    @Test
    @DisplayName("빈 State 검증 실패")
    void testValidateEmptyState() {
        assertFalse(service.validateAndConsumeState(""));
    }
}

