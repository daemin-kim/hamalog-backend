package com.Hamalog.service.diary;

import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.diary.request.MoodDiaryCreateRequest;
import com.Hamalog.dto.diary.response.MoodDiaryListResponse;
import com.Hamalog.dto.diary.response.MoodDiaryResponse;
import com.Hamalog.exception.diary.DiaryAlreadyExistsException;
import com.Hamalog.exception.diary.InvalidDiaryTypeException;
import com.Hamalog.exception.diary.MoodDiaryNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.diary.MoodDiaryRepository;
import com.Hamalog.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MoodDiaryService {

    private final MoodDiaryRepository moodDiaryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public MoodDiaryResponse createMoodDiary(MoodDiaryCreateRequest request) {
        log.info("마음 일기 생성 시작 - memberId: {}, diaryDate: {}", request.getMemberId(), request.getDiaryDate());

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(MemberNotFoundException::new);

        if (moodDiaryRepository.existsByMemberAndDiaryDate(member, request.getDiaryDate())) {
            log.warn("해당 날짜에 이미 일기가 존재함 - memberId: {}, diaryDate: {}",
                    request.getMemberId(), request.getDiaryDate());
            throw new DiaryAlreadyExistsException();
        }

        validateDiaryContent(request);

        MoodDiary moodDiary = createMoodDiaryEntity(member, request);

        MoodDiary savedDiary = moodDiaryRepository.save(moodDiary);
        log.info("마음 일기 생성 완료 - moodDiaryId: {}", savedDiary.getMoodDiaryId());

        return MoodDiaryResponse.from(savedDiary);
    }

    public MoodDiaryResponse getMoodDiary(Long moodDiaryId, Long memberId) {
        log.info("마음 일기 조회 - moodDiaryId: {}, memberId: {}", moodDiaryId, memberId);

        MoodDiary moodDiary = moodDiaryRepository.findByIdAndMemberId(moodDiaryId, memberId)
                .orElseThrow(MoodDiaryNotFoundException::new);

        return MoodDiaryResponse.from(moodDiary);
    }

    public MoodDiaryListResponse getMoodDiariesByMember(Long memberId, int page, int size) {
        log.info("회원 일기 목록 조회 - memberId: {}, page: {}, size: {}", memberId, page, size);

        if (size > 100) {
            size = 100;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Pageable pageable = PageRequest.of(page, size);
        Page<MoodDiary> diaryPage = moodDiaryRepository.findByMemberOrderByDiaryDateDesc(member, pageable);

        List<MoodDiaryResponse> diaries = diaryPage.getContent().stream()
                .map(MoodDiaryResponse::from)
                .collect(Collectors.toList());

        return MoodDiaryListResponse.builder()
                .diaries(diaries)
                .totalCount(diaryPage.getTotalElements())
                .currentPage(diaryPage.getNumber())
                .pageSize(diaryPage.getSize())
                .hasNext(diaryPage.hasNext())
                .hasPrevious(diaryPage.hasPrevious())
                .build();
    }

    public MoodDiaryResponse getMoodDiaryByDate(Long memberId, LocalDate diaryDate) {
        log.info("날짜별 일기 조회 - memberId: {}, diaryDate: {}", memberId, diaryDate);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        MoodDiary moodDiary = moodDiaryRepository.findByMemberAndDiaryDate(member, diaryDate)
                .orElseThrow(MoodDiaryNotFoundException::new);

        return MoodDiaryResponse.from(moodDiary);
    }

    @Transactional
    public void deleteMoodDiary(Long moodDiaryId, Long memberId) {
        log.info("마음 일기 삭제 시작 - moodDiaryId: {}, memberId: {}", moodDiaryId, memberId);

        MoodDiary moodDiary = moodDiaryRepository.findByIdAndMemberId(moodDiaryId, memberId)
                .orElseThrow(MoodDiaryNotFoundException::new);

        moodDiaryRepository.delete(moodDiary);
        log.info("마음 일기 삭제 완료 - moodDiaryId: {}", moodDiaryId);
    }

    private void validateDiaryContent(MoodDiaryCreateRequest request) {
        if (request.getDiaryType() == DiaryType.TEMPLATE) {
            if (!request.isValidTemplateType()) {
                throw new InvalidDiaryTypeException();
            }
        } else if (request.getDiaryType() == DiaryType.FREE_FORM) {
            if (!request.isValidFreeFormType()) {
                throw new InvalidDiaryTypeException();
            }
        } else {
            throw new InvalidDiaryTypeException();
        }
    }

    private MoodDiary createMoodDiaryEntity(Member member, MoodDiaryCreateRequest request) {
        if (request.getDiaryType() == DiaryType.TEMPLATE) {
            return MoodDiary.createTemplateType(
                    member,
                    request.getDiaryDate(),
                    request.getMoodType(),
                    request.getTemplateAnswer1(),
                    request.getTemplateAnswer2(),
                    request.getTemplateAnswer3(),
                    request.getTemplateAnswer4()
            );
        } else {
            return MoodDiary.createFreeFormType(
                    member,
                    request.getDiaryDate(),
                    request.getMoodType(),
                    request.getFreeContent()
            );
        }
    }

    public boolean isOwnerOfDiary(Long moodDiaryId, String loginId) {
        try {
            MoodDiary moodDiary = moodDiaryRepository.findById(moodDiaryId)
                    .orElse(null);
            if (moodDiary == null) {
                return false;
            }
            return moodDiary.getMember().getLoginId().equals(loginId);
        } catch (Exception e) {
            log.warn("마음 일기 소유권 검증 실패 - moodDiaryId: {}, loginId: {}", moodDiaryId, loginId, e);
            return false;
        }
    }

    public boolean isOwnerOfMember(Long memberId, String loginId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElse(null);
            if (member == null) {
                return false;
            }
            return member.getLoginId().equals(loginId);
        } catch (Exception e) {
            log.warn("회원 소유권 검증 실패 - memberId: {}, loginId: {}", memberId, loginId, e);
            return false;
        }
    }
}

