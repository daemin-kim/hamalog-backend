package com.Hamalog.service.sideEffect;

import com.Hamalog.domain.sideEffect.SideEffect;
import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.repository.sideEffect.SideEffectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SideEffectService {

    private final SideEffectRepository sideEffectRepository;

    public SideEffectService(SideEffectRepository sideEffectRepository) {
        this.sideEffectRepository = sideEffectRepository;
    }

    public RecentSideEffectResponse getRecentSideEffects(Long userId) {
        return null;
    }
}
