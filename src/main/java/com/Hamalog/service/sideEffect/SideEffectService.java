package com.Hamalog.service.sideEffect;

import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.sideEffect.SideEffect;
import com.Hamalog.domain.sideEffect.SideEffectDegree;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.domain.sideEffect.SideEffectSideEffectRecord;
import com.Hamalog.dto.sideEffect.request.SideEffectRecordRequest;
import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.Hamalog.repository.sideEffect.SideEffectRepository;
import com.Hamalog.repository.sideEffect.SideEffectSideEffectRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
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
    
    @Transactional
    public void createSideEffectRecord(SideEffectRecordRequest request) {
        // 회원 존재 여부 확인
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(MemberNotFoundException::new);
        
        // SideEffectRecord 생성
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(request.createdAt() != null ? request.createdAt() : LocalDateTime.now())
                .build();
        
        SideEffectRecord savedRecord = sideEffectRecordRepository.save(sideEffectRecord);
        
        // SideEffect와의 연결 관계 생성 및 캐시 업데이트를 위한 side effect 이름 수집
        List<String> newSideEffectNames = request.sideEffects().stream()
                .map(item -> {
                    // SideEffect 존재 여부 확인
                    SideEffect sideEffect = sideEffectRepository.findById(item.sideEffectId())
                            .orElseThrow(() -> new IllegalArgumentException("SideEffect not found with id: " + item.sideEffectId()));
                    
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
        return memberRepository.findById(memberId)
                .map(member -> member.getLoginId().equals(loginId))
                .orElse(false);
    }
}
