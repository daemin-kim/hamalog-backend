package com.Hamalog.service;

import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.service.medication.MedicationScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedicationScheduleServiceTest {

    private MemberRepository memberRepository;
    private MedicationScheduleRepository medicationScheduleRepository;
    private MedicationScheduleService medicationScheduleService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        medicationScheduleRepository = mock(MedicationScheduleRepository.class);
        medicationScheduleService = new MedicationScheduleService(memberRepository, medicationScheduleRepository);
    }

    @Test
    void getMedicationSchedules_정상동작() {
        // given
        Long memberId = 1L;
        List<MedicationSchedule> expected = Arrays.asList(mock(MedicationSchedule.class));
        when(medicationScheduleRepository.findAllByMember_MemberId(memberId)).thenReturn(expected);

        // when
        List<MedicationSchedule> result = medicationScheduleService.getMedicationSchedules(memberId);

        // then
        assertThat(result).isSameAs(expected);
        verify(medicationScheduleRepository).findAllByMember_MemberId(memberId);
    }

    @Test
    void getMedicationSchedule_못찾으면_예외() {
        when(medicationScheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicationScheduleService.getMedicationSchedule(99L))
                .isInstanceOf(MedicationScheduleNotFoundException.class);
    }

    @Test
    void getMedicationSchedule_정상조회() {
        Long id = 123L;
        MedicationSchedule schedule = mock(MedicationSchedule.class);
        when(medicationScheduleRepository.findById(id)).thenReturn(Optional.of(schedule));

        MedicationSchedule result = medicationScheduleService.getMedicationSchedule(id);

        assertThat(result).isSameAs(schedule);
    }

    @Test
    void createMedicationSchedule_회원없음_예외() {
        MedicationScheduleCreateRequest req = new MedicationScheduleCreateRequest(
                99L, "n", "h", "2024-07-01", "memo", "2024-07-01", 1, 1, "SOUND"
        );
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicationScheduleService.createMedicationSchedule(req, null))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void createMedicationSchedule_정상() {
        // given
        Member member = mock(Member.class);
        MedicationScheduleCreateRequest req = new MedicationScheduleCreateRequest(
                1L, "name", "hospital", "2024-07-01", "memo", "2024-07-01", 10, 1, "SOUND"
        );
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        ArgumentCaptor<MedicationSchedule> captor = ArgumentCaptor.forClass(MedicationSchedule.class);
        MedicationSchedule saved = mock(MedicationSchedule.class);
        when(medicationScheduleRepository.save(any())).thenReturn(saved);

        // when
        MedicationSchedule result = medicationScheduleService.createMedicationSchedule(req, "myimg.png");

        // then
        assertThat(result).isSameAs(saved);
        verify(medicationScheduleRepository).save(captor.capture());
        MedicationSchedule ms = captor.getValue();
        assertThat(ms.getName()).isEqualTo("name");
        assertThat(ms.getImagePath()).isEqualTo("myimg.png");
    }

    @Test
    void updateMedicationSchedule_없으면_예외() {
        when(medicationScheduleRepository.findById(99L)).thenReturn(Optional.empty());

        MedicationScheduleUpdateRequest req = mock(MedicationScheduleUpdateRequest.class);
        assertThatThrownBy(() -> medicationScheduleService.updateMedicationSchedule(99L, req))
                .isInstanceOf(MedicationScheduleNotFoundException.class);
    }

    @Test
    void updateMedicationSchedule_정상호출() {
        MedicationSchedule existing = mock(MedicationSchedule.class);
        when(medicationScheduleRepository.findById(2L)).thenReturn(Optional.of(existing));
        MedicationScheduleUpdateRequest req = mock(MedicationScheduleUpdateRequest.class);
        when(req.name()).thenReturn("변경이름");
        when(req.hospitalName()).thenReturn("변경병원");
        when(req.prescriptionDate()).thenReturn(LocalDate.now());
        when(req.memo()).thenReturn("a");
        when(req.startOfAd()).thenReturn(LocalDate.now());
        when(req.prescriptionDays()).thenReturn(5);
        when(req.perDay()).thenReturn(2);
        when(req.alarmType()).thenReturn(AlarmType.SOUND);

        when(medicationScheduleRepository.save(any())).thenReturn(existing);

        MedicationSchedule result = medicationScheduleService.updateMedicationSchedule(2L, req);

        assertThat(result).isSameAs(existing);
        verify(existing).update(
                eq("변경이름"), eq("변경병원"), any(LocalDate.class), eq("a"),
                any(LocalDate.class), eq(5), eq(2), eq(AlarmType.SOUND), eq("")
        );
        verify(medicationScheduleRepository).save(existing);
    }

    @Test
    void deleteMedicationSchedule_정상_삭제() {
        MedicationSchedule ms = mock(MedicationSchedule.class);
        when(medicationScheduleRepository.findById(10L)).thenReturn(Optional.of(ms));

        medicationScheduleService.deleteMedicationSchedule(10L);

        verify(medicationScheduleRepository).delete(ms);
    }

    @Test
    void deleteMedicationSchedule_존재X시_예외() {
        when(medicationScheduleRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> medicationScheduleService.deleteMedicationSchedule(10L))
                .isInstanceOf(MedicationScheduleNotFoundException.class);
    }
}
