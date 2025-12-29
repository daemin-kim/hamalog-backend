package com.Hamalog.service.diary;

import com.Hamalog.domain.diary.DiaryType;
import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.events.DomainEventPublisher;
import com.Hamalog.domain.events.diary.MoodDiaryCreated;
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
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MoodDiaryService {

    private final MoodDiaryRepository moodDiaryRepository;
    private final MemberRepository memberRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public MoodDiaryResponse createMoodDiary(Long memberId, MoodDiaryCreateRequest request) {
        log.info("마음 일기 생성 시작 - memberId: {}, diaryDate: {}", memberId, request.diaryDate());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (moodDiaryRepository.existsByMemberAndDiaryDate(member, request.diaryDate())) {
            log.warn("해당 날짜에 이미 일기가 존재함 - memberId: {}, diaryDate: {}",
                    memberId, request.diaryDate());
            throw new DiaryAlreadyExistsException();
        }

        validateDiaryContent(request);

        MoodDiary moodDiary = createMoodDiaryEntity(member, request);

        MoodDiary savedDiary = moodDiaryRepository.save(moodDiary);
        log.info("마음 일기 생성 완료 - moodDiaryId: {}", savedDiary.getMoodDiaryId());

        // 연속 작성일 계산 (어제 일기가 있으면 연속)
        Integer consecutiveDays = calculateConsecutiveDays(member, request.diaryDate());

        // 도메인 이벤트 발행
        MoodDiaryCreated event = new MoodDiaryCreated(
                savedDiary.getMoodDiaryId(),
                memberId,
                member.getLoginId(),
                savedDiary.getDiaryDate(),
                savedDiary.getMoodType(),
                savedDiary.getDiaryType(),
                consecutiveDays
        );
        domainEventPublisher.publish(event);
        log.debug("Published MoodDiaryCreated event for diary ID: {}", savedDiary.getMoodDiaryId());

        return MoodDiaryResponse.from(savedDiary);
    }

    /**
     * 연속 작성일 계산
     */
    private Integer calculateConsecutiveDays(Member member, LocalDate diaryDate) {
        int consecutive = 1;
        LocalDate checkDate = diaryDate.minusDays(1);

        while (moodDiaryRepository.existsByMemberAndDiaryDate(member, checkDate)) {
            consecutive++;
            checkDate = checkDate.minusDays(1);
            if (consecutive > 365) break;  // 최대 1년까지만 체크
        }

        return consecutive;
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

        return new MoodDiaryListResponse(
                diaries,
                diaryPage.getTotalElements(),
                diaryPage.getNumber(),
                diaryPage.getSize(),
                diaryPage.hasNext(),
                diaryPage.hasPrevious()
        );
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

    @Transactional
    public MoodDiaryResponse updateMoodDiary(Long moodDiaryId, Long memberId, MoodDiaryUpdateRequest request) {
        log.info("마음 일기 수정 시작 - moodDiaryId: {}, memberId: {}", moodDiaryId, memberId);

        MoodDiary moodDiary = moodDiaryRepository.findByIdAndMemberId(moodDiaryId, memberId)
                .orElseThrow(MoodDiaryNotFoundException::new);

        validateUpdateRequest(request);

        if (request.diaryType() == DiaryType.TEMPLATE) {
            moodDiary.updateAsTemplateType(
                    request.moodType(),
                    request.templateAnswer1(),
                    request.templateAnswer2(),
                    request.templateAnswer3(),
                    request.templateAnswer4()
            );
        } else {
            moodDiary.updateAsFreeFormType(
                    request.moodType(),
                    request.freeContent()
            );
        }

        log.info("마음 일기 수정 완료 - moodDiaryId: {}", moodDiaryId);
        return MoodDiaryResponse.from(moodDiary);
    }

    private void validateUpdateRequest(MoodDiaryUpdateRequest request) {
        if (request.diaryType() == DiaryType.TEMPLATE) {
            if (!request.isValidTemplateType()) {
                throw new InvalidDiaryTypeException();
            }
        } else if (request.diaryType() == DiaryType.FREE_FORM) {
            if (!request.isValidFreeFormType()) {
                throw new InvalidDiaryTypeException();
            }
        } else {
            throw new InvalidDiaryTypeException();
        }
    }

    private void validateDiaryContent(MoodDiaryCreateRequest request) {
        if (request.diaryType() == DiaryType.TEMPLATE) {
            if (!request.isValidTemplateType()) {
                throw new InvalidDiaryTypeException();
            }
        } else if (request.diaryType() == DiaryType.FREE_FORM) {
            if (!request.isValidFreeFormType()) {
                throw new InvalidDiaryTypeException();
            }
        } else {
            throw new InvalidDiaryTypeException();
        }
    }

    private MoodDiary createMoodDiaryEntity(Member member, MoodDiaryCreateRequest request) {
        if (request.diaryType() == DiaryType.TEMPLATE) {
            return MoodDiary.createTemplateType(
                    member,
                    request.diaryDate(),
                    request.moodType(),
                    request.templateAnswer1(),
                    request.templateAnswer2(),
                    request.templateAnswer3(),
                    request.templateAnswer4()
            );
        } else {
            return MoodDiary.createFreeFormType(
                    member,
                    request.diaryDate(),
                    request.moodType(),
                    request.freeContent()
            );
        }
    }

    /**
     * 일기 내용 검색
     */
    public MoodDiaryListResponse searchMoodDiaries(Long memberId, String keyword, int page, int size) {
        log.info("마음 일기 검색 - memberId: {}, keyword: {}", memberId, keyword);

        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }

        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<MoodDiary> diaryPage = moodDiaryRepository.searchByKeyword(memberId, keyword, pageable);

        List<MoodDiaryResponse> diaries = diaryPage.getContent().stream()
                .map(MoodDiaryResponse::from)
                .collect(Collectors.toList());

        return new MoodDiaryListResponse(
                diaries,
                diaryPage.getTotalElements(),
                diaryPage.getNumber(),
                diaryPage.getSize(),
                diaryPage.hasNext(),
                diaryPage.hasPrevious()
        );
    }
}
