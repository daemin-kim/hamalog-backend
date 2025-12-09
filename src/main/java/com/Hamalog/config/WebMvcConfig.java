package com.Hamalog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * WebMvc 설정 - React Native 환경 호환성 지원
 *
 * RN에서 FormData의 JSON 파트가 text/plain이나 application/octet-stream으로
 * 전송될 수 있어 이를 처리하기 위한 설정입니다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TextPlainJsonHttpMessageConverter textPlainJsonConverter;

    public WebMvcConfig(TextPlainJsonHttpMessageConverter textPlainJsonConverter) {
        this.textPlainJsonConverter = textPlainJsonConverter;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // TextPlainJsonHttpMessageConverter를 우선순위 높게 추가 (인덱스 0)
        converters.add(0, textPlainJsonConverter);

        // 기존 MappingJackson2HttpMessageConverter를 찾아서 text/plain 지원 추가
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                List<MediaType> supportedMediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());

                // text/plain과 application/octet-stream도 JSON으로 파싱 가능하도록 추가
                if (!supportedMediaTypes.contains(MediaType.TEXT_PLAIN)) {
                    supportedMediaTypes.add(MediaType.TEXT_PLAIN);
                }
                if (!supportedMediaTypes.contains(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))) {
                    supportedMediaTypes.add(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8));
                }

                jacksonConverter.setSupportedMediaTypes(supportedMediaTypes);
                break;
            }
        }
    }
}

