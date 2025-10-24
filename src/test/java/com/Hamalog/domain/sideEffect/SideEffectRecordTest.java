package com.Hamalog.domain.sideEffect;

import com.Hamalog.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SideEffectRecordTest {

    @Test
    @DisplayName("SideEffectRecord 생성 테스트")
    void createSideEffectRecord() {
        // given
        Member mockMember = mock(Member.class);
        LocalDateTime now = LocalDateTime.now();

        // when
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(mockMember)
                .createdAt(now)
                .build();

        // then
        assertThat(sideEffectRecord).isNotNull();
        assertThat(sideEffectRecord.getMember()).isEqualTo(mockMember);
        assertThat(sideEffectRecord.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("SideEffectRecord의 기본 생성자 테스트")
    void noArgsConstructorTest() {
        // when
        SideEffectRecord sideEffectRecord = new SideEffectRecord();

        // then
        assertThat(sideEffectRecord).isNotNull();
        assertThat(sideEffectRecord.getSideEffectRecordId()).isNull();
        assertThat(sideEffectRecord.getMember()).isNull();
        assertThat(sideEffectRecord.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("SideEffectRecord의 전체 생성자 테스트")
    void allArgsConstructorTest() {
        // given
        Long id = 1L;
        Member mockMember = mock(Member.class);
        LocalDateTime now = LocalDateTime.now();

        // when
        SideEffectRecord sideEffectRecord = new SideEffectRecord(id, mockMember, now);

        // then
        assertThat(sideEffectRecord.getSideEffectRecordId()).isEqualTo(id);
        assertThat(sideEffectRecord.getMember()).isEqualTo(mockMember);
        assertThat(sideEffectRecord.getCreatedAt()).isEqualTo(now);
    }
}
