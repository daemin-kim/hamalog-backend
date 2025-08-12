package com.Hamalog.service.sideEffect;

import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SideEffectService {

    private final SideEffectRepository sideEffectRepository;
    private final MemberRepository memberRepository;

    public SideEffectService(SideEffectRepository sideEffectRepository, MemberRepository memberRepository) {
        this.sideEffectRepository = sideEffectRepository;
        this.memberRepository = memberRepository;
    }

    public RecentSideEffectResponse getRecentSideEffects(Long memberId) {
        List<String> names = sideEffectRepository.findRecentSideEffectNames(memberId);

        return new RecentSideEffectResponse(names);
    }
    
    public boolean isOwner(Long memberId, String loginId) {
        return memberRepository.findById(memberId)
                .map(member -> member.getLoginId().equals(loginId))
                .orElse(false);
    }
}
