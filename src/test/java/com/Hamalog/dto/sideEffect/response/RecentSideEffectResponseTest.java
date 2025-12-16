package com.Hamalog.dto.sideEffect.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RecentSideEffectResponse Tests")
class RecentSideEffectResponseTest {

    @Test
    @DisplayName("생성자가 주어진 부작용 목록으로 객체를 생성한다")
    void constructor_WithGivenSideEffects_CreatesObjectCorrectly() {
        // given
        List<String> sideEffects = Arrays.asList("두통", "어지러움", "메스꺼움");

        // when
        RecentSideEffectResponse response = new RecentSideEffectResponse(sideEffects);

        // then
        assertThat(response.recentSideEffect())
            .isNotNull()
            .hasSize(3)
            .containsExactly("두통", "어지러움", "메스꺼움");
    }

    @Test
    @DisplayName("빈 부작용 목록으로 객체를 생성할 수 있다")
    void constructor_WithEmptyList_CreatesObjectWithEmptyList() {
        // given
        List<String> emptyList = Collections.emptyList();

        // when
        RecentSideEffectResponse response = new RecentSideEffectResponse(emptyList);

        // then
        assertThat(response.recentSideEffect())
            .isNotNull()
            .isEmpty();
    }

    @Test
    @DisplayName("부작용 목록이 주어진 경우 해당 값을 정확히 반환한다")
    void recentSideEffect_WithGivenList_ReturnsCorrectValue() {
        // given
        List<String> sideEffects = Arrays.asList("불면증", "피로감");
        RecentSideEffectResponse response = new RecentSideEffectResponse(sideEffects);

        // when
        List<String> result = response.recentSideEffect();

        // then
        assertThat(result)
            .isNotNull()
            .hasSize(2)
            .containsExactly("불면증", "피로감");
    }
}
