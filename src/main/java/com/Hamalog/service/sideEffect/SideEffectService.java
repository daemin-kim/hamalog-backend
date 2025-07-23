package com.Hamalog.service.sideEffect;

import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.repository.sideEffect.SideEffectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SideEffectService {

    private final SideEffectRepository sideEffectRepository;

    public SideEffectService(SideEffectRepository sideEffectRepository) {
        this.sideEffectRepository = sideEffectRepository;
    }

    public RecentSideEffectResponse getRecentSideEffects(Long memberId) {
        List<String> names = sideEffectRepository.findRecentSideEffectNames(memberId);

        return new RecentSideEffectResponse(names);
    }
}
