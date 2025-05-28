package com.Hamalog.service.sideEffect;

import com.Hamalog.repository.sideEffect.SideEffectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SideEffectService {

    private final SideEffectRepository sideEffectRepository;

    public SideEffectService(SideEffectRepository sideEffectRepository) {
        this.sideEffectRepository = sideEffectRepository;
    }
}
