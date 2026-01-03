package com.Hamalog.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 알림 설정 빈 등록
 * Discord 웹훅 알림 관련 프로퍼티를 Bean으로 등록합니다.
 */
@Configuration
@EnableConfigurationProperties(AlertProperties.class)
public class AlertConfig {
    // AlertProperties를 Bean으로 등록
}
