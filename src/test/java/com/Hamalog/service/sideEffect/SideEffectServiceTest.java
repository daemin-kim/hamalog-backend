package com.Hamalog.service.sideEffect;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.events.DomainEventPublisher;
import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.sideEffect.SideEffect;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.domain.sideEffect.SideEffectSideEffectRecord;
import com.Hamalog.dto.sideEffect.request.SideEffectRecordRequest;
import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.exception.sideEffect.SideEffectNotFoundException;
import com.Hamalog.exception.validation.InvalidInputException;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.Hamalog.repository.sideEffect.SideEffectRepository;
import com.Hamalog.repository.sideEffect.SideEffectSideEffectRecordRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SideEffectService Tests")
class SideEffectServiceTest {

    @Mock
    private SideEffectRepository sideEffectRepository;

    @Mock
    private SideEffectRecordRepository sideEffectRecordRepository;

    @Mock
    private SideEffectSideEffectRecordRepository sideEffectSideEffectRecordRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private RecentSideEffectCacheService cacheService;

    @InjectMocks
    private SideEffectService sideEffectService;

    private Member mockMember;
    private SideEffect mockSideEffect1;
    private SideEffect mockSideEffect2;
    private SideEffectRecord mockRecord;
    private SideEffectRecordRequest.SideEffectItem sideEffectItem1;
    private SideEffectRecordRequest.SideEffectItem sideEffectItem2;
    private SideEffectRecordRequest createRequest;

    @BeforeEach
    void setUp() {
        // Create mock objects without stubbing - set up behavior in individual tests as needed
        mockMember = mock(Member.class);
        mockSideEffect1 = mock(SideEffect.class);
        mockSideEffect2 = mock(SideEffect.class);
        mockRecord = mock(SideEffectRecord.class);

        // Create request items
        sideEffectItem1 = new SideEffectRecordRequest.SideEffectItem(1L, 1); // degree as Integer
        sideEffectItem2 = new SideEffectRecordRequest.SideEffectItem(2L, 2); // degree as Integer

        createRequest = new SideEffectRecordRequest(
                1L, // memberId
                LocalDateTime.now(), // createdAt
                null, // linkedMedicationScheduleId
                Arrays.asList(sideEffectItem1, sideEffectItem2) // sideEffects
        );
    }

    @Test
    @DisplayName("Should get recent side effects with cache enabled - cache hit")
    void getRecentSideEffects_WithCacheEnabled_CacheHit() {
        // given
        Long memberId = 1L;
        List<String> cachedNames = Arrays.asList("Headache", "Nausea");
        SideEffectService serviceWithCache = new SideEffectService(
                sideEffectRepository, sideEffectRecordRepository, sideEffectSideEffectRecordRepository,
                memberRepository, medicationScheduleRepository, domainEventPublisher, cacheService
        );
        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(cacheService.getRecentSideEffects(memberId)).thenReturn(cachedNames);

        // when
        RecentSideEffectResponse result = serviceWithCache.getRecentSideEffects(memberId);

        // then
        assertThat(result.recentSideEffect()).isEqualTo(cachedNames);
        verify(memberRepository).existsById(memberId);
        verify(cacheService).getRecentSideEffects(memberId);
        verify(sideEffectRepository, never()).findRecentSideEffectNames(any());
        verify(cacheService, never()).refreshRecentSideEffects(any(), any());
    }

    @Test
    @DisplayName("Should get recent side effects with cache enabled - cache miss")
    void getRecentSideEffects_WithCacheEnabled_CacheMiss() {
        // given
        Long memberId = 1L;
        List<String> dbNames = Arrays.asList("Headache", "Nausea");
        SideEffectService serviceWithCache = new SideEffectService(
                sideEffectRepository, sideEffectRecordRepository, sideEffectSideEffectRecordRepository,
                memberRepository, medicationScheduleRepository, domainEventPublisher, cacheService
        );
        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(cacheService.getRecentSideEffects(memberId)).thenReturn(Collections.emptyList());
        when(sideEffectRepository.findRecentSideEffectNames(memberId)).thenReturn(dbNames);

        // when
        RecentSideEffectResponse result = serviceWithCache.getRecentSideEffects(memberId);

        // then
        assertThat(result.recentSideEffect()).isEqualTo(dbNames);
        verify(memberRepository).existsById(memberId);
        verify(cacheService).getRecentSideEffects(memberId);
        verify(sideEffectRepository).findRecentSideEffectNames(memberId);
        verify(cacheService).refreshRecentSideEffects(memberId, dbNames);
    }

    @Test
    @DisplayName("Should get recent side effects with cache enabled - empty database result")
    void getRecentSideEffects_WithCacheEnabled_EmptyDbResult() {
        // given
        Long memberId = 1L;
        List<String> emptyNames = Collections.emptyList();
        SideEffectService serviceWithCache = new SideEffectService(
                sideEffectRepository, sideEffectRecordRepository, sideEffectSideEffectRecordRepository,
                memberRepository, medicationScheduleRepository, domainEventPublisher, cacheService
        );
        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(cacheService.getRecentSideEffects(memberId)).thenReturn(Collections.emptyList());
        when(sideEffectRepository.findRecentSideEffectNames(memberId)).thenReturn(emptyNames);

        // when
        RecentSideEffectResponse result = serviceWithCache.getRecentSideEffects(memberId);

        // then
        assertThat(result.recentSideEffect()).isEmpty();
        verify(memberRepository).existsById(memberId);
        verify(cacheService).getRecentSideEffects(memberId);
        verify(sideEffectRepository).findRecentSideEffectNames(memberId);
        verify(cacheService, never()).refreshRecentSideEffects(any(), any());
    }

    @Test
    @DisplayName("Should get recent side effects with cache disabled")
    void getRecentSideEffects_WithCacheDisabled() {
        // given
        Long memberId = 1L;
        List<String> dbNames = Arrays.asList("Headache", "Nausea");
        SideEffectService serviceWithoutCache = new SideEffectService(
                sideEffectRepository, sideEffectRecordRepository, sideEffectSideEffectRecordRepository,
                memberRepository, medicationScheduleRepository, domainEventPublisher, null
        );
        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(sideEffectRepository.findRecentSideEffectNames(memberId)).thenReturn(dbNames);

        // when
        RecentSideEffectResponse result = serviceWithoutCache.getRecentSideEffects(memberId);

        // then
        assertThat(result.recentSideEffect()).isEqualTo(dbNames);
        verify(memberRepository).existsById(memberId);
        verify(sideEffectRepository).findRecentSideEffectNames(memberId);
        verifyNoInteractions(cacheService);
    }

    @Test
    @DisplayName("Should create side effect record successfully")
    void createSideEffectRecord_Success() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));
        when(sideEffectRecordRepository.save(any(SideEffectRecord.class))).thenReturn(mockRecord);
        
        // Set up mock behaviors for SideEffects with different IDs
        when(mockSideEffect1.getSideEffectId()).thenReturn(1L);
        when(mockSideEffect1.getName()).thenReturn("Headache");
        when(mockSideEffect2.getSideEffectId()).thenReturn(2L);
        when(mockSideEffect2.getName()).thenReturn("Nausea");
        when(mockRecord.getSideEffectRecordId()).thenReturn(10L);
        
        when(sideEffectRepository.findAllById(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(mockSideEffect1, mockSideEffect2));
        when(sideEffectSideEffectRecordRepository.save(any(SideEffectSideEffectRecord.class)))
                .thenReturn(mock(SideEffectSideEffectRecord.class));

        SideEffectService serviceWithCache = new SideEffectService(
                sideEffectRepository, sideEffectRecordRepository, sideEffectSideEffectRecordRepository,
                memberRepository, medicationScheduleRepository, domainEventPublisher, cacheService
        );

        // when
        serviceWithCache.createSideEffectRecord(createRequest);

        // then
        verify(memberRepository).findById(1L);
        verify(sideEffectRecordRepository).save(any(SideEffectRecord.class));
        verify(sideEffectRepository).findAllById(Arrays.asList(1L, 2L));
        verify(sideEffectSideEffectRecordRepository, times(2)).save(any(SideEffectSideEffectRecord.class));
        verify(cacheService, times(2)).addRecentSideEffect(eq(1L), anyString());
        verify(domainEventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should throw exception when member not found")
    void createSideEffectRecord_MemberNotFound() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sideEffectService.createSideEffectRecord(createRequest))
                .isInstanceOf(MemberNotFoundException.class);

        verify(memberRepository).findById(1L);
        verify(sideEffectRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when side effect not found")
    void createSideEffectRecord_SideEffectNotFound() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));
        when(sideEffectRecordRepository.save(any(SideEffectRecord.class))).thenReturn(mockRecord);
        when(sideEffectRepository.findAllById(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(mockSideEffect2)); // Only return side effect with ID 2, missing ID 1
        when(mockSideEffect2.getSideEffectId()).thenReturn(2L);

        // when & then
        assertThatThrownBy(() -> sideEffectService.createSideEffectRecord(createRequest))
                .isInstanceOf(SideEffectNotFoundException.class);

        verify(memberRepository).findById(1L);
        verify(sideEffectRecordRepository).save(any(SideEffectRecord.class));
        verify(sideEffectRepository).findAllById(Arrays.asList(1L, 2L));
    }

    @Test
    @DisplayName("Should create side effect record without cache service")
    void createSideEffectRecord_WithoutCache() {
        // given
        SideEffectService serviceWithoutCache = new SideEffectService(
                sideEffectRepository, sideEffectRecordRepository, sideEffectSideEffectRecordRepository,
                memberRepository, medicationScheduleRepository, domainEventPublisher, null
        );
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));
        when(sideEffectRecordRepository.save(any(SideEffectRecord.class))).thenReturn(mockRecord);
        
        // Set up mock behaviors for SideEffects with different IDs
        when(mockSideEffect1.getSideEffectId()).thenReturn(1L);
        when(mockSideEffect1.getName()).thenReturn("Headache");
        when(mockSideEffect2.getSideEffectId()).thenReturn(2L);
        when(mockSideEffect2.getName()).thenReturn("Nausea");
        when(mockRecord.getSideEffectRecordId()).thenReturn(10L);
        
        when(sideEffectRepository.findAllById(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(mockSideEffect1, mockSideEffect2));

        // when
        serviceWithoutCache.createSideEffectRecord(createRequest);

        // then
        verify(memberRepository).findById(1L);
        verify(sideEffectRecordRepository).save(any(SideEffectRecord.class));
        verify(sideEffectRepository).findAllById(Arrays.asList(1L, 2L));
        verify(sideEffectSideEffectRecordRepository, times(2)).save(any(SideEffectSideEffectRecord.class));
        verifyNoInteractions(cacheService);
    }

    @Test
    @DisplayName("Should handle empty side effects list in create request")
    void createSideEffectRecord_EmptySideEffectsList() {
        // given
        SideEffectRecordRequest emptyRequest = new SideEffectRecordRequest(
                1L,
                LocalDateTime.now(),
                null,
                Collections.emptyList()
        );

        SideEffectService serviceWithCache = new SideEffectService(
                sideEffectRepository, sideEffectRecordRepository, sideEffectSideEffectRecordRepository,
                memberRepository, medicationScheduleRepository, domainEventPublisher, cacheService
        );

        // when & then
        assertThatThrownBy(() -> serviceWithCache.createSideEffectRecord(emptyRequest))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining(ErrorCode.EMPTY_SIDE_EFFECT_LIST.getMessage());

        verify(memberRepository, never()).findById(any());
        verify(sideEffectRecordRepository, never()).save(any(SideEffectRecord.class));
        verify(sideEffectRepository, never()).findAllById(any());
        verify(sideEffectSideEffectRecordRepository, never()).save(any());
        verify(cacheService, never()).addRecentSideEffect(any(), any());
    }

    @Test
    @DisplayName("Should return true when member is owner")
    void isOwner_MemberIsOwner() {
        // given
        Long memberId = 1L;
        String loginId = "testuser";
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
        when(mockMember.getLoginId()).thenReturn("testuser");

        // when
        boolean result = sideEffectService.isOwner(memberId, loginId);

        // then
        assertThat(result).isTrue();
        verify(memberRepository).findById(memberId);
        verify(mockMember).getLoginId();
    }

    @Test
    @DisplayName("Should return false when member is not owner")
    void isOwner_MemberIsNotOwner() {
        // given
        Long memberId = 1L;
        String loginId = "differentuser";
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
        when(mockMember.getLoginId()).thenReturn("testuser");

        // when
        boolean result = sideEffectService.isOwner(memberId, loginId);

        // then
        assertThat(result).isFalse();
        verify(memberRepository).findById(memberId);
        verify(mockMember).getLoginId();
    }

    @Test
    @DisplayName("Should return false when member not found")
    void isOwner_MemberNotFound() {
        // given
        Long memberId = 999L;
        String loginId = "testuser";
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when
        boolean result = sideEffectService.isOwner(memberId, loginId);

        // then
        assertThat(result).isFalse();
        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("Should use current time when createdAt is null")
    void createSideEffectRecord_NullCreatedAt() {
        // given
        SideEffectRecordRequest requestWithNullDate = new SideEffectRecordRequest(
                1L,
                null, // createdAt is null
                null, // linkedMedicationScheduleId
                Arrays.asList(sideEffectItem1)
        );
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));
        when(sideEffectRecordRepository.save(any(SideEffectRecord.class))).thenReturn(mockRecord);
        
        // Set up mock behavior for SideEffect
        when(mockSideEffect1.getSideEffectId()).thenReturn(1L);
        when(mockSideEffect1.getName()).thenReturn("Headache");
        when(mockRecord.getSideEffectRecordId()).thenReturn(10L);
        
        when(sideEffectRepository.findAllById(Arrays.asList(1L))).thenReturn(Arrays.asList(mockSideEffect1));

        SideEffectService serviceWithCache = new SideEffectService(
                sideEffectRepository, sideEffectRecordRepository, sideEffectSideEffectRecordRepository,
                memberRepository, medicationScheduleRepository, domainEventPublisher, cacheService
        );

        // when
        serviceWithCache.createSideEffectRecord(requestWithNullDate);

        // then
        ArgumentCaptor<SideEffectRecord> recordCaptor = ArgumentCaptor.forClass(SideEffectRecord.class);
        verify(sideEffectRecordRepository).save(recordCaptor.capture());
        
        // Verify that a record was saved (createdAt should be set automatically)
        verify(memberRepository).findById(1L);
        verify(sideEffectRepository).findAllById(Arrays.asList(1L));
        verify(cacheService).addRecentSideEffect(eq(1L), eq("Headache"));
    }
}