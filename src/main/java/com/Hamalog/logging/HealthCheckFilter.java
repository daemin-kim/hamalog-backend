package com.Hamalog.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * 헬스체크 관련 로그를 필터링하는 TurboFilter
 * /actuator 경로에 대한 요청 로그를 제외합니다.
 */
public class HealthCheckFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
                              String format, Object[] params, Throwable t) {
        if (format == null) {
            return FilterReply.NEUTRAL;
        }

        // /actuator 경로가 포함된 로그 메시지 필터링
        if (format.contains("/actuator")) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}
