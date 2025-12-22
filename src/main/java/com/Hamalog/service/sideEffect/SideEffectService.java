package com.Hamalog.service.sideEffect;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.sideEffect.SideEffect;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.domain.sideEffect.SideEffectSideEffectRecord;
import com.Hamalog.dto.sideEffect.request.SideEffectRecordRequest;
import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.dto.sideEffect.response.SideEffectRecordListResponse;
import com.Hamalog.dto.sideEffect.response.SideEffectRecordResponse;
import com.Hamalog.dto.sideEffect.response.SideEffectRecordResponse.SideEffectDetailResponse;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.exception.sideEffect.SideEffectNotFoundException;
import com.Hamalog.exception.validation.InvalidInputException;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.Hamalog.repository.sideEffect.SideEffectRepository;
import com.Hamalog.repository.sideEffect.SideEffectSideEffectRecordRepository;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class SideEffectService {

    private final SideEffectRepository sideEffectRepository;
    private final SideEffectRecordRepository sideEffectRecordRepository;
    private final SideEffectSideEffectRecordRepository sideEffectSideEffectRecordRepository;
    private final MemberRepository memberRepository;
    private final RecentSideEffectCacheService cacheService;

    public SideEffectService(SideEffectRepository sideEffectRepository,
                           SideEffectRecordRepository sideEffectRecordRepository,
                           SideEffectSideEffectRecordRepository sideEffectSideEffectRecordRepository,
                           MemberRepository memberRepository,
                           @Autowired(required = false) RecentSideEffectCacheService cacheService) {
        this.sideEffectRepository = sideEffectRepository;
        this.sideEffectRecordRepository = sideEffectRecordRepository;
        this.sideEffectSideEffectRecordRepository = sideEffectSideEffectRecordRepository;
        this.memberRepository = memberRepository;
        this.cacheService = cacheService;
    }

    public RecentSideEffectResponse getRecentSideEffects(Long memberId) {
        // 입력값 검증
        if (memberId == null || memberId <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        // 회원 존재 여부 검증
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }

        List<String> names;
        
        // Redis 캐시가 활성화된 경우 캐시 우선 조회
        if (cacheService != null) {
            names = cacheService.getRecentSideEffects(memberId);
            
            // 캐시에 데이터가 없거나 비어있으면 데이터베이스에서 조회 후 캐시 갱신
            if (names.isEmpty()) {
                names = sideEffectRepository.findRecentSideEffectNames(memberId);
                
                // 데이터베이스에서 조회한 결과로 캐시 갱신
                if (!names.isEmpty()) {
                    cacheService.refreshRecentSideEffects(memberId, names);
                }
            }
        } else {
            // Redis가 비활성화된 경우 데이터베이스 직접 조회
            names = sideEffectRepository.findRecentSideEffectNames(memberId);
        }

        return new RecentSideEffectResponse(names);
    }
    
    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEMBER,
            paramName = "memberId",
            source = RequireResourceOwnership.ParameterSource.REQUEST_BODY,
            bodyField = "memberId",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    @Transactional(rollbackFor = {Exception.class})
    public void createSideEffectRecord(SideEffectRecordRequest request) {
        // 입력값 검증
        validateSideEffectRecordRequest(request);

        // 회원 존재 여부 확인
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(MemberNotFoundException::new);
        
        // 생성 시간 검증
        LocalDateTime createdAt = request.createdAt() != null ? request.createdAt() : LocalDateTime.now();
        validateCreatedAt(createdAt);

        // SideEffectRecord 생성
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(createdAt)
                .build();
        
        SideEffectRecord savedRecord = sideEffectRecordRepository.save(sideEffectRecord);
        
        // N+1 문제 해결: SideEffect 일괄 조회
        List<Long> sideEffectIds = request.sideEffects().stream()
                .map(item -> item.sideEffectId())
                .toList();
        
        // IN 쿼리로 모든 SideEffect를 한 번에 조회
        List<SideEffect> sideEffects = sideEffectRepository.findAllById(sideEffectIds);
        
        // 존재하지 않는 SideEffect ID 검증
        if (sideEffects.size() != sideEffectIds.size()) {
            List<Long> foundIds = sideEffects.stream()
                    .map(SideEffect::getSideEffectId)
                    .toList();
            List<Long> missingIds = sideEffectIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            log.warn("SideEffect IDs not found: {}", missingIds);
            throw new SideEffectNotFoundException();
        }

        // ID를 키로 하는 Map으로 변환하여 빠른 조회 가능
        Map<Long, SideEffect> sideEffectMap = sideEffects.stream()
                .collect(Collectors.toMap(SideEffect::getSideEffectId, Function.identity()));
        
        // SideEffect와의 연결 관계 생성 및 캐시 업데이트를 위한 side effect 이름 수집
        List<String> newSideEffectNames = request.sideEffects().stream()
                .map(item -> {
                    // 부작용 정도 검증
                    validateSideEffectDegree(item.degree());

                    // Map에서 SideEffect 조회 (N+1 문제 해결됨)
                    SideEffect sideEffect = sideEffectMap.get(item.sideEffectId());
                    if (sideEffect == null) {
                        log.error("SideEffect not found with id: {}", item.sideEffectId());
                        throw new SideEffectNotFoundException(item.sideEffectId());
                    }
                    
                    // SideEffectSideEffectRecord 생성
                    SideEffectSideEffectRecord linkRecord = SideEffectSideEffectRecord.builder()
                            .sideEffectRecordId(savedRecord.getSideEffectRecordId())
                            .sideEffectId(item.sideEffectId())
                            .sideEffectRecord(savedRecord)
                            .sideEffect(sideEffect)
                            .degree(item.degree())
                            .build();
                    
                    sideEffectSideEffectRecordRepository.save(linkRecord);
                    
                    // 캐시 업데이트를 위해 부작용 이름 반환
                    return sideEffect.getName();
                })
                .toList();
        
        // Redis 캐시 업데이트 (각 부작용 이름을 캐시에 추가)
        if (cacheService != null && !newSideEffectNames.isEmpty()) {
            for (String sideEffectName : newSideEffectNames) {
                cacheService.addRecentSideEffect(request.memberId(), sideEffectName);
            }
        }
    }
    
    public boolean isOwner(Long memberId, String loginId) {
        if (memberId == null || loginId == null) {
            return false;
        }
        return memberRepository.findById(memberId)
                .map(member -> member.getLoginId().equals(loginId))
                .orElse(false);
    }

    /**
     * 부작용 기록 목록 조회 (페이징)
     */
    public SideEffectRecordListResponse getSideEffectRecords(Long memberId, int page, int size) {
        log.info("부작용 기록 목록 조회 - memberId: {}, page: {}, size: {}", memberId, page, size);

        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }

        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<SideEffectRecord> recordPage = sideEffectRecordRepository
                .findByMember_MemberIdOrderByCreatedAtDesc(memberId, pageable);

        List<SideEffectRecordResponse> records = recordPage.getContent().stream()
                .map(this::toSideEffectRecordResponse)
                .collect(Collectors.toList());

        return new SideEffectRecordListResponse(
                records,
                recordPage.getTotalElements(),
                recordPage.getNumber(),
                recordPage.getSize(),
                recordPage.hasNext(),
                recordPage.hasPrevious()
        );
    }

    /**
     * 부작용 기록 상세 조회
     */
    public SideEffectRecordResponse getSideEffectRecord(Long recordId) {
        log.info("부작용 기록 상세 조회 - recordId: {}", recordId);

        SideEffectRecord record = sideEffectRecordRepository.findByIdWithMember(recordId)
                .orElseThrow(SideEffectNotFoundException::new);

        return toSideEffectRecordResponse(record);
    }

    /**
     * 부작용 기록 삭제
     */
    @Transactional
    public void deleteSideEffectRecord(Long recordId, Long memberId) {
        log.info("부작용 기록 삭제 - recordId: {}, memberId: {}", recordId, memberId);

        if (!sideEffectRecordRepository.existsByIdAndMemberId(recordId, memberId)) {
            throw new SideEffectNotFoundException();
        }

        // 연관된 SideEffectSideEffectRecord 먼저 삭제
        sideEffectSideEffectRecordRepository.deleteBySideEffectRecordId(recordId);

        // SideEffectRecord 삭제
        sideEffectRecordRepository.deleteById(recordId);

        // 캐시 갱신
        if (cacheService != null) {
            List<String> updatedNames = sideEffectRepository.findRecentSideEffectNames(memberId);
            cacheService.refreshRecentSideEffects(memberId, updatedNames);
        }

        log.info("부작용 기록 삭제 완료 - recordId: {}", recordId);
    }

    private SideEffectRecordResponse toSideEffectRecordResponse(SideEffectRecord record) {
        List<SideEffectSideEffectRecord> sideEffectRecords = sideEffectSideEffectRecordRepository
                .findBySideEffectRecordIdWithSideEffect(record.getSideEffectRecordId());

        List<SideEffectDetailResponse> details = sideEffectRecords.stream()
                .map(ssr -> new SideEffectDetailResponse(
                        ssr.getSideEffect().getSideEffectId(),
                        ssr.getSideEffect().getName(),
                        ssr.getDegree()
                ))
                .collect(Collectors.toList());

        return new SideEffectRecordResponse(
                record.getSideEffectRecordId(),
                record.getMember().getMemberId(),
                record.getCreatedAt(),
                details
        );
    }

    // ========== Private Validation Methods ==========

    /**
     * 부작용 기록 요청 데이터 검증
     */
    private void validateSideEffectRecordRequest(SideEffectRecordRequest request) {
        if (request == null) {
            throw new InvalidInputException(ErrorCode.BAD_REQUEST);
        }

        if (request.memberId() == null || request.memberId() <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        if (request.sideEffects() == null || request.sideEffects().isEmpty()) {
            log.warn("Side effect list is empty for member {}", request.memberId());
            throw new InvalidInputException(ErrorCode.EMPTY_SIDE_EFFECT_LIST);
        }

        // 부작용 목록 크기 제한 (DoS 방지)
        if (request.sideEffects().size() > 50) {
            log.warn("Side effect list size {} exceeds maximum allowed (50)", request.sideEffects().size());
            throw new InvalidInputException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 부작용 정도 검증 - 1-5 사이여야 함
     */
    private void validateSideEffectDegree(Integer degree) {
        if (degree == null || degree < 1 || degree > 5) {
            log.warn("Invalid side effect degree: {}", degree);
            throw new InvalidInputException(ErrorCode.INVALID_SIDE_EFFECT_DEGREE);
        }
    }

    /**
     * 생성 시간 검증 - 미래 시간은 불가
     */
    private void validateCreatedAt(LocalDateTime createdAt) {
        if (createdAt.isAfter(LocalDateTime.now())) {
            log.warn("Created time {} is in the future", createdAt);
            throw new InvalidInputException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
