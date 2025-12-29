package com.Hamalog.service.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.diary.MoodType;
import com.Hamalog.domain.events.DomainEventPublisher;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.diary.request.MoodDiaryCreateRequest;
import com.Hamalog.dto.diary.request.MoodDiaryUpdateRequest;
import com.Hamalog.dto.diary.response.MoodDiaryListResponse;
import com.Hamalog.dto.diary.response.MoodDiaryResponse;
import com.Hamalog.exception.diary.DiaryAlreadyExistsException;
import com.Hamalog.exception.diary.InvalidDiaryTypeException;
import com.Hamalog.exception.diary.MoodDiaryNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.diary.MoodDiaryRepository;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoodDiaryService 테스트")
class MoodDiaryServiceTest {

    @Mock
    private MoodDiaryRepository moodDiaryRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private MoodDiaryService moodDiaryService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .memberId(1L)
                .loginId("user@example.com")
                .password("encoded")
                .name("사용자")
                .phoneNumber("01012345678")
                .nickName("테스터")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("템플릿 마음 일기 생성 성공")
    void createMoodDiary_Template_Success() {
        // given
        MoodDiaryCreateRequest request = new MoodDiaryCreateRequest(
                1L,
                LocalDate.of(2025, 12, 1),
                MoodType.HAPPY,
                DiaryType.TEMPLATE,
                "A1", "A2", "A3", "A4",
                null
        );

        MoodDiary savedDiary = buildTemplateDiary(10L, request.diaryDate());

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(moodDiaryRepository.existsByMemberAndDiaryDate(member, request.diaryDate())).thenReturn(false);
        when(moodDiaryRepository.save(any(MoodDiary.class))).thenReturn(savedDiary);

        // when
        MoodDiaryResponse response = moodDiaryService.createMoodDiary(1L, request);

        // then
        assertThat(response.moodDiaryId()).isEqualTo(10L);
        assertThat(response.diaryType()).isEqualTo(DiaryType.TEMPLATE);
        assertThat(response.templateAnswer1()).isEqualTo("A1");

        verify(moodDiaryRepository).save(any(MoodDiary.class));
    }

    @Test
    @DisplayName("자유 형식 마음 일기 생성 성공")
    void createMoodDiary_FreeForm_Success() {
        // given
        MoodDiaryCreateRequest request = new MoodDiaryCreateRequest(
                1L,
                LocalDate.of(2025, 12, 2),
                MoodType.PEACEFUL,
                DiaryType.FREE_FORM,
                null, null, null, null,
                "평온한 하루"
        );

        MoodDiary savedDiary = buildFreeFormDiary(11L, request.diaryDate(), "평온한 하루");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(moodDiaryRepository.existsByMemberAndDiaryDate(member, request.diaryDate())).thenReturn(false);
        when(moodDiaryRepository.save(any(MoodDiary.class))).thenReturn(savedDiary);

        // when
        MoodDiaryResponse response = moodDiaryService.createMoodDiary(1L, request);

        // then
        assertThat(response.diaryType()).isEqualTo(DiaryType.FREE_FORM);
        assertThat(response.freeContent()).isEqualTo("평온한 하루");
    }

    @Test
    @DisplayName("하루 1회 제한 위반 시 예외 발생")
    void createMoodDiary_Duplicate_ThrowsException() {
        // given
        MoodDiaryCreateRequest request = new MoodDiaryCreateRequest(
                1L,
                LocalDate.now(),
                MoodType.HAPPY,
                DiaryType.TEMPLATE,
                "A1", "A2", "A3", "A4",
                null
        );

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(moodDiaryRepository.existsByMemberAndDiaryDate(member, request.diaryDate())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> moodDiaryService.createMoodDiary(1L, request))
                .isInstanceOf(DiaryAlreadyExistsException.class);

        verify(moodDiaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("유효하지 않은 템플릿 답변 시 예외 발생")
    void createMoodDiary_InvalidTemplate_ThrowsException() {
        // given
        MoodDiaryCreateRequest request = new MoodDiaryCreateRequest(
                1L,
                LocalDate.now(),
                MoodType.HAPPY,
                DiaryType.TEMPLATE,
                "A1", null, null, null,
                null
        );

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(moodDiaryRepository.existsByMemberAndDiaryDate(eq(member), any())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> moodDiaryService.createMoodDiary(1L, request))
                .isInstanceOf(InvalidDiaryTypeException.class);
    }

    @Test
    @DisplayName("회원 정보를 찾지 못하면 예외 발생")
    void createMoodDiary_MemberNotFound() {
        // given
        MoodDiaryCreateRequest request = new MoodDiaryCreateRequest(
                2L,
                LocalDate.now(),
                MoodType.HAPPY,
                DiaryType.FREE_FORM,
                null, null, null, null,
                "content"
        );

        when(memberRepository.findById(2L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> moodDiaryService.createMoodDiary(2L, request))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("마음 일기 단건 조회 성공")
    void getMoodDiary_Success() {
        // given
        MoodDiary diary = buildTemplateDiary(5L, LocalDate.of(2025, 12, 1));
        when(moodDiaryRepository.findByIdAndMemberId(5L, 1L)).thenReturn(Optional.of(diary));

        // when
        MoodDiaryResponse response = moodDiaryService.getMoodDiary(5L, 1L);

        // then
        assertThat(response.moodDiaryId()).isEqualTo(5L);
        assertThat(response.memberId()).isEqualTo(member.getMemberId());
    }

    @Test
    @DisplayName("마음 일기 단건 조회 실패")
    void getMoodDiary_NotFound() {
        // given
        when(moodDiaryRepository.findByIdAndMemberId(99L, 1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> moodDiaryService.getMoodDiary(99L, 1L))
                .isInstanceOf(MoodDiaryNotFoundException.class);
    }

    @Test
    @DisplayName("마음 일기 목록 조회 시 페이지 크기 최대 100으로 제한")
    void getMoodDiariesByMember_SizeCapped() {
        // given
        LocalDate date = LocalDate.of(2025, 12, 1);
        MoodDiary diary = buildTemplateDiary(3L, date);
        Page<MoodDiary> page = new PageImpl<>(List.of(diary), PageRequest.of(0, 100), 1);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(moodDiaryRepository.findByMemberOrderByDiaryDateDesc(eq(member), any(Pageable.class)))
                .thenReturn(page);

        // when
        MoodDiaryListResponse response = moodDiaryService.getMoodDiariesByMember(1L, 0, 200);

        // then
        assertThat(response.diaries()).hasSize(1);
        assertThat(response.pageSize()).isEqualTo(100);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(moodDiaryRepository).findByMemberOrderByDiaryDateDesc(eq(member), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("특정 날짜 마음 일기 조회 성공")
    void getMoodDiaryByDate_Success() {
        // given
        LocalDate diaryDate = LocalDate.of(2025, 12, 3);
        MoodDiary diary = buildTemplateDiary(7L, diaryDate);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(moodDiaryRepository.findByMemberAndDiaryDate(member, diaryDate)).thenReturn(Optional.of(diary));

        // when
        MoodDiaryResponse response = moodDiaryService.getMoodDiaryByDate(1L, diaryDate);

        // then
        assertThat(response.diaryDate()).isEqualTo(diaryDate);
    }

    @Test
    @DisplayName("특정 날짜 마음 일기 조회 실패")
    void getMoodDiaryByDate_NotFound() {
        // given
        LocalDate diaryDate = LocalDate.of(2025, 12, 3);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(moodDiaryRepository.findByMemberAndDiaryDate(member, diaryDate)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> moodDiaryService.getMoodDiaryByDate(1L, diaryDate))
                .isInstanceOf(MoodDiaryNotFoundException.class);
    }

    @Test
    @DisplayName("마음 일기 수정 성공 - 템플릿에서 자유 형식으로 변경")
    void updateMoodDiary_TemplateToFreeForm_Success() {
        // given
        MoodDiary diary = buildTemplateDiary(10L, LocalDate.of(2025, 12, 1));
        MoodDiaryUpdateRequest request = new MoodDiaryUpdateRequest(
                MoodType.SAD,
                DiaryType.FREE_FORM,
                null, null, null, null,
                "오늘은 슬픈 하루였습니다."
        );

        when(moodDiaryRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.of(diary));

        // when
        MoodDiaryResponse response = moodDiaryService.updateMoodDiary(10L, 1L, request);

        // then
        assertThat(response.moodType()).isEqualTo(MoodType.SAD);
        assertThat(response.diaryType()).isEqualTo(DiaryType.FREE_FORM);
        assertThat(response.freeContent()).isEqualTo("오늘은 슬픈 하루였습니다.");
        assertThat(response.templateAnswer1()).isNull();
    }

    @Test
    @DisplayName("마음 일기 수정 성공 - 자유 형식에서 템플릿으로 변경")
    void updateMoodDiary_FreeFormToTemplate_Success() {
        // given
        MoodDiary diary = buildFreeFormDiary(11L, LocalDate.of(2025, 12, 2), "기존 내용");
        MoodDiaryUpdateRequest request = new MoodDiaryUpdateRequest(
                MoodType.EXCITED,
                DiaryType.TEMPLATE,
                "새로운 A1", "새로운 A2", "새로운 A3", "새로운 A4",
                null
        );

        when(moodDiaryRepository.findByIdAndMemberId(11L, 1L)).thenReturn(Optional.of(diary));

        // when
        MoodDiaryResponse response = moodDiaryService.updateMoodDiary(11L, 1L, request);

        // then
        assertThat(response.moodType()).isEqualTo(MoodType.EXCITED);
        assertThat(response.diaryType()).isEqualTo(DiaryType.TEMPLATE);
        assertThat(response.templateAnswer1()).isEqualTo("새로운 A1");
        assertThat(response.freeContent()).isNull();
    }

    @Test
    @DisplayName("마음 일기 수정 실패 - 일기를 찾을 수 없음")
    void updateMoodDiary_NotFound() {
        // given
        MoodDiaryUpdateRequest request = new MoodDiaryUpdateRequest(
                MoodType.HAPPY,
                DiaryType.FREE_FORM,
                null, null, null, null,
                "내용"
        );

        when(moodDiaryRepository.findByIdAndMemberId(99L, 1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> moodDiaryService.updateMoodDiary(99L, 1L, request))
                .isInstanceOf(MoodDiaryNotFoundException.class);
    }

    @Test
    @DisplayName("마음 일기 수정 실패 - 유효하지 않은 템플릿 답변")
    void updateMoodDiary_InvalidTemplate() {
        // given
        MoodDiary diary = buildTemplateDiary(12L, LocalDate.of(2025, 12, 3));
        MoodDiaryUpdateRequest request = new MoodDiaryUpdateRequest(
                MoodType.HAPPY,
                DiaryType.TEMPLATE,
                "A1만 있음", null, null, null,
                null
        );

        when(moodDiaryRepository.findByIdAndMemberId(12L, 1L)).thenReturn(Optional.of(diary));

        // when & then
        assertThatThrownBy(() -> moodDiaryService.updateMoodDiary(12L, 1L, request))
                .isInstanceOf(InvalidDiaryTypeException.class);
    }

    @Test
    @DisplayName("마음 일기 수정 실패 - 자유 형식인데 내용 없음")
    void updateMoodDiary_InvalidFreeForm() {
        // given
        MoodDiary diary = buildFreeFormDiary(13L, LocalDate.of(2025, 12, 4), "기존 내용");
        MoodDiaryUpdateRequest request = new MoodDiaryUpdateRequest(
                MoodType.HAPPY,
                DiaryType.FREE_FORM,
                null, null, null, null,
                "   "  // 공백만 있는 경우
        );

        when(moodDiaryRepository.findByIdAndMemberId(13L, 1L)).thenReturn(Optional.of(diary));

        // when & then
        assertThatThrownBy(() -> moodDiaryService.updateMoodDiary(13L, 1L, request))
                .isInstanceOf(InvalidDiaryTypeException.class);
    }

    @Test
    @DisplayName("마음 일기 삭제 성공")
    void deleteMoodDiary_Success() {
        // given
        MoodDiary diary = buildTemplateDiary(9L, LocalDate.now());
        when(moodDiaryRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(diary));

        // when
        moodDiaryService.deleteMoodDiary(9L, 1L);

        // then
        verify(moodDiaryRepository).delete(diary);
    }

    @Test
    @DisplayName("마음 일기 삭제 실패")
    void deleteMoodDiary_NotFound() {
        // given
        when(moodDiaryRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> moodDiaryService.deleteMoodDiary(9L, 1L))
                .isInstanceOf(MoodDiaryNotFoundException.class);
    }

    // 소유권 검증 테스트는 ResourceOwnershipValidatorTest로 이동됨

    private MoodDiary buildTemplateDiary(Long id, LocalDate diaryDate) {
        return MoodDiary.builder()
                .moodDiaryId(id)
                .member(member)
                .diaryDate(diaryDate)
                .moodType(MoodType.HAPPY)
                .diaryType(DiaryType.TEMPLATE)
                .templateAnswer1("A1")
                .templateAnswer2("A2")
                .templateAnswer3("A3")
                .templateAnswer4("A4")
                .createdAt(LocalDateTime.of(2025, 12, 1, 10, 0))
                .build();
    }

    private MoodDiary buildFreeFormDiary(Long id, LocalDate diaryDate, String content) {
        return MoodDiary.builder()
                .moodDiaryId(id)
                .member(member)
                .diaryDate(diaryDate)
                .moodType(MoodType.PEACEFUL)
                .diaryType(DiaryType.FREE_FORM)
                .freeContent(content)
                .createdAt(LocalDateTime.of(2025, 12, 2, 10, 0))
                .build();
    }
}
