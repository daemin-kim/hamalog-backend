package com.Hamalog.domain.events.member;

import com.Hamalog.domain.events.DomainEvent;
import lombok.Getter;

/**
 * 회원 삭제 완료 이벤트
 * DB 트랜잭션 완료 후 후속 작업(Redis 토큰 블랙리스트 등)을 비동기로 처리하기 위한 이벤트
 */
@Getter
public class MemberDeletedEvent extends DomainEvent {
    
    private final String loginId;
    private final String token;
    private final Long memberId;
    
    public MemberDeletedEvent(String loginId, String token, Long memberId) {
        super();
        this.loginId = loginId;
        this.token = token;
        this.memberId = memberId;
    }
    
    @Override
    public String getAggregateId() {
        return loginId;
    }
    
    @Override
    public String toString() {
        return String.format("MemberDeletedEvent{loginId='%s', memberId=%d, occurredOn=%s}", 
                           loginId, memberId, getOccurredOn());
    }
}