package com.Hamalog.service.auth;

import com.Hamalog.aop.CachingAspect.CacheEvict;
import com.Hamalog.aop.CachingAspect.Cacheable;
import com.Hamalog.domain.member.Member;
import com.Hamalog.repository.member.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 정보 캐싱 서비스
 * 자주 조회되는 회원 정보를 캐싱하여 DB 부하를 줄입니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberCacheService {

    private static final String CACHE_NAME = "member";
    private static final long CACHE_TTL_SECONDS = 600; // 10분

    private final MemberRepository memberRepository;

    /**
     * 로그인 ID로 회원 조회 (캐시 적용)
     *
     * @param loginId 로그인 ID (이메일)
     * @return 회원 정보
     */
    @Cacheable(value = CACHE_NAME, key = "'loginId:' + #loginId", ttl = CACHE_TTL_SECONDS)
    public Optional<Member> findByLoginId(String loginId) {
        log.debug("[MEMBER_CACHE] Cache miss - fetching member by loginId from DB");
        return memberRepository.findByLoginId(loginId);
    }

    /**
     * 회원 ID로 회원 조회 (캐시 적용)
     *
     * @param memberId 회원 ID
     * @return 회원 정보
     */
    @Cacheable(value = CACHE_NAME, key = "'memberId:' + #memberId", ttl = CACHE_TTL_SECONDS)
    public Optional<Member> findById(Long memberId) {
        log.debug("[MEMBER_CACHE] Cache miss - fetching member by memberId from DB");
        return memberRepository.findById(memberId);
    }

    /**
     * 회원 정보 변경 시 캐시 무효화
     *
     * @param loginId 로그인 ID
     * @param memberId 회원 ID
     */
    @CacheEvict(value = CACHE_NAME, key = "'loginId:' + #loginId")
    public void evictByLoginId(String loginId, Long memberId) {
        log.info("[MEMBER_CACHE] Evicting cache for loginId: {}, memberId: {}", loginId, memberId);
    }

    /**
     * 회원 ID로 캐시 무효화
     *
     * @param memberId 회원 ID
     */
    @CacheEvict(value = CACHE_NAME, key = "'memberId:' + #memberId")
    public void evictByMemberId(Long memberId) {
        log.info("[MEMBER_CACHE] Evicting cache for memberId: {}", memberId);
    }
}
