package com.Hamalog.service.medication;

import com.Hamalog.domain.events.DomainEventPublisher;
import com.Hamalog.domain.events.medication.MedicationScheduleCreated;
import com.Hamalog.domain.events.medication.MedicationScheduleDeleted;
import com.Hamalog.domain.events.medication.MedicationScheduleUpdated;
import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationScheduleService Tests")
class MedicationScheduleServiceTest {

    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;
    
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private MedicationScheduleService medicationScheduleService;

    private Member mockMember;
    private MedicationSchedule mockSchedule;
    private MedicationScheduleCreateRequest createRequest;
    private MedicationScheduleUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Create mock member
        mockMember = mock(Member.class);
        lenient().when(mockMember.getMemberId()).thenReturn(1L);
        lenient().when(mockMember.getLoginId()).thenReturn("testUser");

        // Create mock schedule
        mockSchedule = mock(MedicationSchedule.class);
        lenient().when(mockSchedule.getMedicationScheduleId()).thenReturn(1L);
        lenient().when(mockSchedule.getMember()).thenReturn(mockMember);
        lenient().when(mockSchedule.getName()).thenReturn("Test Medicine");
        lenient().when(mockSchedule.getHospitalName()).thenReturn("Test Hospital");
        lenient().when(mockSchedule.getPrescriptionDate()).thenReturn(LocalDate.of(2024, 1, 1));
        lenient().when(mockSchedule.getMemo()).thenReturn("Test memo");
        lenient().when(mockSchedule.getStartOfAd()).thenReturn(LocalDate.of(2024, 1, 1));
        lenient().when(mockSchedule.getPrescriptionDays()).thenReturn(30);
        lenient().when(mockSchedule.getPerDay()).thenReturn(3);
        lenient().when(mockSchedule.getAlarmType()).thenReturn(AlarmType.SOUND);

        // Create request objects
        createRequest = new MedicationScheduleCreateRequest(
                1L, // memberId
                "Test Medicine",
                "Test Hospital",
                "2024-01-01", // prescriptionDate
                "Test memo",
                "2024-01-01", // startOfAd
                30, // prescriptionDays
                3, // perDay
                "SOUND" // alarmType
        );

        updateRequest = new MedicationScheduleUpdateRequest(
                "Updated Medicine",
                "Updated Hospital",
                LocalDate.of(2024, 2, 1),
                "Updated memo",
                LocalDate.of(2024, 2, 1),
                45,
                2,
                AlarmType.SOUND
        );
    }

    @Test
    @DisplayName("Should get medication schedules by member ID")
    void getMedicationSchedules_WithValidMemberId_ShouldReturnSchedules() {
        // given
        Long memberId = 1L;
        List<MedicationSchedule> expectedSchedules = Arrays.asList(mockSchedule);
        when(medicationScheduleRepository.findAllByMember_MemberId(memberId))
                .thenReturn(expectedSchedules);

        // when
        List<MedicationSchedule> result = medicationScheduleService.getMedicationSchedules(memberId);

        // then
        assertThat(result).isEqualTo(expectedSchedules);
        verify(medicationScheduleRepository).findAllByMember_MemberId(memberId);
    }

    @Test
    @DisplayName("Should get medication schedules with pagination")
    void getMedicationSchedules_WithPagination_ShouldReturnPagedSchedules() {
        // given
        Long memberId = 1L;
        Pageable pageable = mock(Pageable.class);
        Page<MedicationSchedule> expectedPage = new PageImpl<>(Arrays.asList(mockSchedule));
        when(medicationScheduleRepository.findByMember_MemberId(memberId, pageable))
                .thenReturn(expectedPage);

        // when
        Page<MedicationSchedule> result = medicationScheduleService.getMedicationSchedules(memberId, pageable);

        // then
        assertThat(result).isEqualTo(expectedPage);
        verify(medicationScheduleRepository).findByMember_MemberId(memberId, pageable);
    }

    @Test
    @DisplayName("Should get medication schedule by ID")
    void getMedicationSchedule_WithValidId_ShouldReturnSchedule() {
        // given
        Long scheduleId = 1L;
        when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));

        // when
        MedicationSchedule result = medicationScheduleService.getMedicationSchedule(scheduleId);

        // then
        assertThat(result).isEqualTo(mockSchedule);
        verify(medicationScheduleRepository).findById(scheduleId);
    }

    @Test
    @DisplayName("Should throw exception when medication schedule not found")
    void getMedicationSchedule_WithInvalidId_ShouldThrowException() {
        // given
        Long scheduleId = 999L;
        when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> medicationScheduleService.getMedicationSchedule(scheduleId))
                .isInstanceOf(MedicationScheduleNotFoundException.class);
        
        verify(medicationScheduleRepository).findById(scheduleId);
    }

    @Test
    @DisplayName("Should create medication schedule successfully")
    void createMedicationSchedule_WithValidRequest_ShouldCreateAndPublishEvent() {
        // given
        when(memberRepository.findById(createRequest.memberId())).thenReturn(Optional.of(mockMember));
        when(medicationScheduleRepository.save(any(MedicationSchedule.class))).thenReturn(mockSchedule);

        // when
        MedicationSchedule result = medicationScheduleService.createMedicationSchedule(createRequest);

        // then
        assertThat(result).isEqualTo(mockSchedule);
        verify(memberRepository).findById(createRequest.memberId());
        verify(medicationScheduleRepository).save(any(MedicationSchedule.class));
        
        // Verify domain event publishing
        ArgumentCaptor<MedicationScheduleCreated> eventCaptor = ArgumentCaptor.forClass(MedicationScheduleCreated.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        
        MedicationScheduleCreated publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getMedicationScheduleId()).isEqualTo(mockSchedule.getMedicationScheduleId());
        assertThat(publishedEvent.getMemberId()).isEqualTo(mockMember.getMemberId());
        assertThat(publishedEvent.getMemberLoginId()).isEqualTo(mockMember.getLoginId());
        assertThat(publishedEvent.getName()).isEqualTo(mockSchedule.getName());
    }

    @Test
    @DisplayName("Should throw exception when member not found during creation")
    void createMedicationSchedule_WithInvalidMemberId_ShouldThrowException() {
        // given
        when(memberRepository.findById(createRequest.memberId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> medicationScheduleService.createMedicationSchedule(createRequest))
                .isInstanceOf(MemberNotFoundException.class);
        
        verify(memberRepository).findById(createRequest.memberId());
        verify(medicationScheduleRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Should update medication schedule successfully")
    void updateMedicationSchedule_WithValidRequest_ShouldUpdateAndPublishEvent() {
        // given
        Long scheduleId = 1L;
        when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(medicationScheduleRepository.save(mockSchedule)).thenReturn(mockSchedule);

        // when
        MedicationSchedule result = medicationScheduleService.updateMedicationSchedule(scheduleId, updateRequest);

        // then
        assertThat(result).isEqualTo(mockSchedule);
        verify(medicationScheduleRepository).findById(scheduleId);
        verify(mockSchedule).update(
                updateRequest.name(),
                updateRequest.hospitalName(),
                updateRequest.prescriptionDate(),
                updateRequest.memo(),
                updateRequest.startOfAd(),
                updateRequest.prescriptionDays(),
                updateRequest.perDay(),
                updateRequest.alarmType()
        );
        verify(medicationScheduleRepository).save(mockSchedule);
        
        // Verify domain event publishing
        ArgumentCaptor<MedicationScheduleUpdated> eventCaptor = ArgumentCaptor.forClass(MedicationScheduleUpdated.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        
        MedicationScheduleUpdated publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getMedicationScheduleId()).isEqualTo(mockSchedule.getMedicationScheduleId());
        assertThat(publishedEvent.getMemberId()).isEqualTo(mockMember.getMemberId());
        assertThat(publishedEvent.getMemberLoginId()).isEqualTo(mockMember.getLoginId());
        assertThat(publishedEvent.getName()).isEqualTo(mockSchedule.getName());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent schedule")
    void updateMedicationSchedule_WithInvalidId_ShouldThrowException() {
        // given
        Long scheduleId = 999L;
        when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> medicationScheduleService.updateMedicationSchedule(scheduleId, updateRequest))
                .isInstanceOf(MedicationScheduleNotFoundException.class);
        
        verify(medicationScheduleRepository).findById(scheduleId);
        verify(medicationScheduleRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Should delete medication schedule successfully")
    void deleteMedicationSchedule_WithValidId_ShouldDeleteAndPublishEvent() {
        // given
        Long scheduleId = 1L;
        when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));

        // when
        medicationScheduleService.deleteMedicationSchedule(scheduleId);

        // then
        verify(medicationScheduleRepository).findById(scheduleId);
        verify(medicationScheduleRepository).delete(mockSchedule);
        
        // Verify domain event publishing
        ArgumentCaptor<MedicationScheduleDeleted> eventCaptor = ArgumentCaptor.forClass(MedicationScheduleDeleted.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        
        MedicationScheduleDeleted publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getMedicationScheduleId()).isEqualTo(mockSchedule.getMedicationScheduleId());
        assertThat(publishedEvent.getMemberId()).isEqualTo(mockMember.getMemberId());
        assertThat(publishedEvent.getMemberLoginId()).isEqualTo(mockMember.getLoginId());
        assertThat(publishedEvent.getName()).isEqualTo(mockSchedule.getName());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent schedule")
    void deleteMedicationSchedule_WithInvalidId_ShouldThrowException() {
        // given
        Long scheduleId = 999L;
        when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> medicationScheduleService.deleteMedicationSchedule(scheduleId))
                .isInstanceOf(MedicationScheduleNotFoundException.class);
        
        verify(medicationScheduleRepository).findById(scheduleId);
        verify(medicationScheduleRepository, never()).delete(any());
        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Should return true when user is owner of member")
    void isOwner_WithValidOwner_ShouldReturnTrue() {
        // given
        Long memberId = 1L;
        String loginId = "testUser";
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));

        // when
        boolean result = medicationScheduleService.isOwner(memberId, loginId);

        // then
        assertThat(result).isTrue();
        verify(memberRepository).findById(memberId);
        verify(mockMember).getLoginId();
    }

    @Test
    @DisplayName("Should return false when user is not owner")
    void isOwner_WithInvalidOwner_ShouldReturnFalse() {
        // given
        Long memberId = 1L;
        String loginId = "otherUser";
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));

        // when
        boolean result = medicationScheduleService.isOwner(memberId, loginId);

        // then
        assertThat(result).isFalse();
        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("Should return false when member not found")
    void isOwner_WithNonExistentMember_ShouldReturnFalse() {
        // given
        Long memberId = 999L;
        String loginId = "testUser";
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when
        boolean result = medicationScheduleService.isOwner(memberId, loginId);

        // then
        assertThat(result).isFalse();
        verify(memberRepository).findById(memberId);
    }
}