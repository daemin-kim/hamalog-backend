package com.Hamalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * text/plain Content-Type으로 전송된 JSON 문자열을 Java 객체로 변환하는 Converter
 *
 * React Native 환경에서 FormData의 Blob/JSON 파트가 text/plain으로 전송되는 경우를 처리합니다.
 */
@Component
public class TextPlainJsonHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private final ObjectMapper objectMapper;

    public TextPlainJsonHttpMessageConverter(ObjectMapper objectMapper) {
        super(
            MediaType.TEXT_PLAIN,
            new MediaType("text", "plain", StandardCharsets.UTF_8)
        );
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean supports(@NonNull Class<?> clazz) {
        // Record 클래스 또는 DTO 클래스를 지원
        return clazz.isRecord() || clazz.getPackageName().contains("dto");
    }

    @Override
    @NonNull
    protected Object readInternal(@NonNull Class<?> clazz, @NonNull HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        String body = new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);

        // 빈 문자열이면 빈 객체 생성 시도
        if (body.trim().isEmpty()) {
            throw new HttpMessageNotReadableException(
                "Request body is empty",
                inputMessage
            );
        }

        try {
            return objectMapper.readValue(body, clazz);
        } catch (Exception e) {
            throw new HttpMessageNotReadableException(
                "Failed to parse text/plain body as JSON: " + e.getMessage(),
                inputMessage
            );
        }
    }

    @Override
    protected void writeInternal(@NonNull Object o, @NonNull HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        String json = objectMapper.writeValueAsString(o);
        outputMessage.getBody().write(json.getBytes(StandardCharsets.UTF_8));
    }
}
