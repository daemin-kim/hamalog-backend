package com.Hamalog.repository.notification;

import com.Hamalog.domain.notification.NotificationSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 알림 설정 Repository
 */
@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    /**
     * 회원 ID로 알림 설정 조회
     */
    Optional<NotificationSettings> findByMember_MemberId(Long memberId);

    /**
     * 회원 ID로 알림 설정 존재 여부 확인
     */
    boolean existsByMember_MemberId(Long memberId);

    /**
     * 회원 ID로 알림 설정 삭제
     */
    void deleteByMember_MemberId(Long memberId);
}
