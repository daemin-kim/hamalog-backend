package com.Hamalog.domain.events.auth;

import com.Hamalog.domain.events.DomainEvent;
import lombok.Getter;

/**
 * 로그인 성공 이벤트
 * 사용자 로그인 성공 시 발행되어 로그인 이력 저장 및 후속 작업 처리에 사용
 */
@Getter
public class LoginSucceeded extends DomainEvent {

    private final Long memberId;
    private final String loginId;
    private final String ipAddress;
    private final String userAgent;
    private final String deviceType;

    public LoginSucceeded(
            Long memberId,
            String loginId,
            String ipAddress,
            String userAgent,
            String deviceType
    ) {
        super();
        this.memberId = memberId;
        this.loginId = loginId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceType = deviceType;
    }

    @Override
    public String getAggregateId() {
        return memberId.toString();
    }

    @Override
    public String toString() {
        return String.format(
                "LoginSucceeded{memberId=%d, loginId=%s, device=%s, occurredOn=%s}",
                memberId, loginId, deviceType, getOccurredOn()
        );
    }
}
