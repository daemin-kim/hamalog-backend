package com.Hamalog.dto.sideEffect.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SideEffectRecordRequest Tests")
class SideEffectRecordRequestTest {

    @Test
    @DisplayName("Should create valid SideEffectRecordRequest with valid data")
    void constructor_WithValidData_ShouldCreateValidRequest() {
        // given
        Long memberId = 1L;
        LocalDateTime createdAt = LocalDateTime.now();
        List<SideEffectRecordRequest.SideEffectItem> sideEffects = Arrays.asList(
                new SideEffectRecordRequest.SideEffectItem(1L, 3),
                new SideEffectRecordRequest.SideEffectItem(2L, 5)
        );

        // when
        SideEffectRecordRequest request = new SideEffectRecordRequest(memberId, createdAt, sideEffects);

        // then
        assertThat(request.memberId()).isEqualTo(memberId);
        assertThat(request.createdAt()).isEqualTo(createdAt);
        assertThat(request.sideEffects()).hasSize(2);
        assertThat(request.sideEffects().get(0).sideEffectId()).isEqualTo(1L);
        assertThat(request.sideEffects().get(0).degree()).isEqualTo(3);
        assertThat(request.sideEffects().get(1).sideEffectId()).isEqualTo(2L);
        assertThat(request.sideEffects().get(1).degree()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should create SideEffectRecordRequest with null memberId")
    void constructor_WithNullMemberId_ShouldCreateRequest() {
        // given
        Long memberId = null;
        LocalDateTime createdAt = LocalDateTime.now();
        List<SideEffectRecordRequest.SideEffectItem> sideEffects = Collections.emptyList();

        // when
        SideEffectRecordRequest request = new SideEffectRecordRequest(memberId, createdAt, sideEffects);

        // then
        assertThat(request.memberId()).isNull();
        assertThat(request.createdAt()).isEqualTo(createdAt);
        assertThat(request.sideEffects()).isEmpty();
    }

    @Test
    @DisplayName("Should create SideEffectRecordRequest with null createdAt")
    void constructor_WithNullCreatedAt_ShouldCreateRequest() {
        // given
        Long memberId = 1L;
        LocalDateTime createdAt = null;
        List<SideEffectRecordRequest.SideEffectItem> sideEffects = Collections.emptyList();

        // when
        SideEffectRecordRequest request = new SideEffectRecordRequest(memberId, createdAt, sideEffects);

        // then
        assertThat(request.memberId()).isEqualTo(memberId);
        assertThat(request.createdAt()).isNull();
        assertThat(request.sideEffects()).isEmpty();
    }

    @Test
    @DisplayName("Should create SideEffectRecordRequest with null sideEffects list")
    void constructor_WithNullSideEffects_ShouldCreateRequest() {
        // given
        Long memberId = 1L;
        LocalDateTime createdAt = LocalDateTime.now();
        List<SideEffectRecordRequest.SideEffectItem> sideEffects = null;

        // when
        SideEffectRecordRequest request = new SideEffectRecordRequest(memberId, createdAt, sideEffects);

        // then
        assertThat(request.memberId()).isEqualTo(memberId);
        assertThat(request.createdAt()).isEqualTo(createdAt);
        assertThat(request.sideEffects()).isNull();
    }

    @Test
    @DisplayName("Should create SideEffectRecordRequest with empty sideEffects list")
    void constructor_WithEmptySideEffects_ShouldCreateRequest() {
        // given
        Long memberId = 1L;
        LocalDateTime createdAt = LocalDateTime.now();
        List<SideEffectRecordRequest.SideEffectItem> sideEffects = Collections.emptyList();

        // when
        SideEffectRecordRequest request = new SideEffectRecordRequest(memberId, createdAt, sideEffects);

        // then
        assertThat(request.memberId()).isEqualTo(memberId);
        assertThat(request.createdAt()).isEqualTo(createdAt);
        assertThat(request.sideEffects()).isEmpty();
    }

    @Test
    @DisplayName("Should maintain equality and hashCode contract")
    void record_ShouldMaintainEqualityAndHashCode() {
        // given
        Long memberId = 1L;
        LocalDateTime createdAt = LocalDateTime.of(2023, 10, 14, 12, 0);
        List<SideEffectRecordRequest.SideEffectItem> sideEffects = Arrays.asList(
                new SideEffectRecordRequest.SideEffectItem(1L, 3)
        );

        SideEffectRecordRequest request1 = new SideEffectRecordRequest(memberId, createdAt, sideEffects);
        SideEffectRecordRequest request2 = new SideEffectRecordRequest(memberId, createdAt, sideEffects);

        // then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1).isEqualTo(request1); // reflexive
    }

    @Test
    @DisplayName("Should not be equal when fields differ")
    void record_WithDifferentFields_ShouldNotBeEqual() {
        // given
        LocalDateTime createdAt = LocalDateTime.now();
        List<SideEffectRecordRequest.SideEffectItem> sideEffects = Collections.emptyList();

        SideEffectRecordRequest request1 = new SideEffectRecordRequest(1L, createdAt, sideEffects);
        SideEffectRecordRequest request2 = new SideEffectRecordRequest(2L, createdAt, sideEffects);

        // then
        assertThat(request1).isNotEqualTo(request2);
        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should contain field information in toString")
    void toString_ShouldContainFieldInformation() {
        // given
        Long memberId = 1L;
        LocalDateTime createdAt = LocalDateTime.of(2023, 10, 14, 12, 0);
        List<SideEffectRecordRequest.SideEffectItem> sideEffects = Arrays.asList(
                new SideEffectRecordRequest.SideEffectItem(1L, 3)
        );

        SideEffectRecordRequest request = new SideEffectRecordRequest(memberId, createdAt, sideEffects);

        // when
        String toStringResult = request.toString();

        // then
        assertThat(toStringResult).contains("SideEffectRecordRequest");
        assertThat(toStringResult).contains("memberId=1");
        assertThat(toStringResult).contains("createdAt=2023-10-14T12:00");
        assertThat(toStringResult).contains("sideEffects");
    }

    @Test
    @DisplayName("Should handle large list of side effects")
    void constructor_WithLargeSideEffectsList_ShouldCreateRequest() {
        // given
        Long memberId = 1L;
        LocalDateTime createdAt = LocalDateTime.now();
        List<SideEffectRecordRequest.SideEffectItem> sideEffects = new ArrayList<>();
        
        // Add 100 side effect items
        for (int i = 1; i <= 100; i++) {
            sideEffects.add(new SideEffectRecordRequest.SideEffectItem((long) i, i % 10 + 1));
        }

        // when
        SideEffectRecordRequest request = new SideEffectRecordRequest(memberId, createdAt, sideEffects);

        // then
        assertThat(request.memberId()).isEqualTo(memberId);
        assertThat(request.createdAt()).isEqualTo(createdAt);
        assertThat(request.sideEffects()).hasSize(100);
        assertThat(request.sideEffects().get(0).sideEffectId()).isEqualTo(1L);
        assertThat(request.sideEffects().get(99).sideEffectId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should create valid SideEffectItem with valid data")
    void sideEffectItem_WithValidData_ShouldCreateValidItem() {
        // given
        Long sideEffectId = 1L;
        Integer degree = 5;

        // when
        SideEffectRecordRequest.SideEffectItem item = new SideEffectRecordRequest.SideEffectItem(sideEffectId, degree);

        // then
        assertThat(item.sideEffectId()).isEqualTo(sideEffectId);
        assertThat(item.degree()).isEqualTo(degree);
    }

    @Test
    @DisplayName("Should create SideEffectItem with null sideEffectId")
    void sideEffectItem_WithNullSideEffectId_ShouldCreateItem() {
        // given
        Long sideEffectId = null;
        Integer degree = 3;

        // when
        SideEffectRecordRequest.SideEffectItem item = new SideEffectRecordRequest.SideEffectItem(sideEffectId, degree);

        // then
        assertThat(item.sideEffectId()).isNull();
        assertThat(item.degree()).isEqualTo(degree);
    }

    @Test
    @DisplayName("Should create SideEffectItem with null degree")
    void sideEffectItem_WithNullDegree_ShouldCreateItem() {
        // given
        Long sideEffectId = 1L;
        Integer degree = null;

        // when
        SideEffectRecordRequest.SideEffectItem item = new SideEffectRecordRequest.SideEffectItem(sideEffectId, degree);

        // then
        assertThat(item.sideEffectId()).isEqualTo(sideEffectId);
        assertThat(item.degree()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10, Integer.MAX_VALUE})
    @DisplayName("Should handle various degree values in SideEffectItem")
    void sideEffectItem_WithVariousDegreeValues_ShouldCreateItem(Integer degree) {
        // given
        Long sideEffectId = 1L;

        // when
        SideEffectRecordRequest.SideEffectItem item = new SideEffectRecordRequest.SideEffectItem(sideEffectId, degree);

        // then
        assertThat(item.sideEffectId()).isEqualTo(sideEffectId);
        assertThat(item.degree()).isEqualTo(degree);
    }

    @Test
    @DisplayName("Should handle negative degree values in SideEffectItem")
    void sideEffectItem_WithNegativeDegree_ShouldCreateItem() {
        // given
        Long sideEffectId = 1L;
        Integer degree = -1;

        // when
        SideEffectRecordRequest.SideEffectItem item = new SideEffectRecordRequest.SideEffectItem(sideEffectId, degree);

        // then
        assertThat(item.sideEffectId()).isEqualTo(sideEffectId);
        assertThat(item.degree()).isEqualTo(degree);
    }

    @Test
    @DisplayName("Should maintain equality and hashCode contract for SideEffectItem")
    void sideEffectItem_ShouldMaintainEqualityAndHashCode() {
        // given
        Long sideEffectId = 1L;
        Integer degree = 5;

        SideEffectRecordRequest.SideEffectItem item1 = new SideEffectRecordRequest.SideEffectItem(sideEffectId, degree);
        SideEffectRecordRequest.SideEffectItem item2 = new SideEffectRecordRequest.SideEffectItem(sideEffectId, degree);

        // then
        assertThat(item1).isEqualTo(item2);
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        assertThat(item1).isEqualTo(item1); // reflexive
    }

    @Test
    @DisplayName("Should not be equal when SideEffectItem fields differ")
    void sideEffectItem_WithDifferentFields_ShouldNotBeEqual() {
        // given
        SideEffectRecordRequest.SideEffectItem item1 = new SideEffectRecordRequest.SideEffectItem(1L, 3);
        SideEffectRecordRequest.SideEffectItem item2 = new SideEffectRecordRequest.SideEffectItem(1L, 5);

        // then
        assertThat(item1).isNotEqualTo(item2);
        assertThat(item1.hashCode()).isNotEqualTo(item2.hashCode());
    }

    @Test
    @DisplayName("Should contain field information in SideEffectItem toString")
    void sideEffectItem_ToString_ShouldContainFieldInformation() {
        // given
        Long sideEffectId = 1L;
        Integer degree = 5;
        SideEffectRecordRequest.SideEffectItem item = new SideEffectRecordRequest.SideEffectItem(sideEffectId, degree);

        // when
        String toStringResult = item.toString();

        // then
        assertThat(toStringResult).contains("SideEffectItem");
        assertThat(toStringResult).contains("sideEffectId=1");
        assertThat(toStringResult).contains("degree=5");
    }
}