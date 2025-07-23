package com.Hamalog.service.sideEffect;

import com.Hamalog.domain.sideEffect.SideEffect;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.domain.sideEffect.SideEffectSideEffectRecord;
import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.Hamalog.repository.sideEffect.SideEffectRepository;
import com.Hamalog.repository.sideEffect.SideEffectSideEffectRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SideEffectService {

    private final SideEffectRepository sideEffectRepository;
    private final SideEffectRecordRepository sideEffectRecordRepository;
    private final SideEffectSideEffectRecordRepository sideEffectSideEffectRecordRepository;

    public SideEffectService(
            SideEffectRepository sideEffectRepository,
            SideEffectRecordRepository sideEffectRecordRepository,
            SideEffectSideEffectRecordRepository sideEffectSideEffectRecordRepository
    ) {
        this.sideEffectRepository = sideEffectRepository;
        this.sideEffectRecordRepository = sideEffectRecordRepository;
        this.sideEffectSideEffectRecordRepository = sideEffectSideEffectRecordRepository;
    }

    public RecentSideEffectResponse getRecentSideEffects(Long userId) {
        List<SideEffectRecord> records = sideEffectRecordRepository
                .findTop5ByMember_MemberIdOrderByCreatedAtDesc(userId);

        List<Long> recordIds = records.stream()
                .map(SideEffectRecord::getSideEffectRecordId)
                .toList();

        List<SideEffectSideEffectRecord> mappingEntities =
                sideEffectSideEffectRecordRepository.findBySideEffectRecordIdIn(recordIds);

        List<Long> sideEffectIds = mappingEntities.stream()
                .map(SideEffectSideEffectRecord::getSideEffectId)
                .distinct()
                .toList();

        List<SideEffect> effects = sideEffectRepository.findAllById(sideEffectIds);
        List<String> sideEffectNames = effects.stream()
                .map(SideEffect::getName)
                .toList();

        return new RecentSideEffectResponse(sideEffectNames);
    }
}
