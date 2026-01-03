package com.Hamalog.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 메시지 큐 설정 빈 등록
 */
@Configuration
@EnableConfigurationProperties(MessageQueueProperties.class)
public class MessageQueueConfig {
    // MessageQueueProperties를 Bean으로 등록
}
