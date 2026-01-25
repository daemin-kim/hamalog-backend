package com.Hamalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    @Value("${server.description:Local Development Server}")
    private String serverDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hamalog API")
                        .description(
                                "복약 관리 시스템 API 문서입니다. JWT 토큰 기반 인증과 OAuth2(카카오) 로그인을 지원하며, 복약 스케줄링, 복약 기록, 부작용 추적 기능을 제공합니다.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Hamalog Development Team")
                                .email("support@hamalog.com")
                                .url("https://github.com/hamalog"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(new Server().url(serverUrl).description(serverDescription)))
                .tags(createApiTags())
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 토큰을 Authorization 헤더에 Bearer 형태로 전달합니다."))
                        .addSecuritySchemes(
                                "csrfToken",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-CSRF-TOKEN")
                                        .description("CSRF 토큰을 X-CSRF-TOKEN 헤더에 전달합니다. POST, PUT, DELETE 요청 시 필수입니다. /auth/csrf-token에서 발급받을 수 있습니다."))
                        .addSchemas("ErrorResponse", createErrorResponseSchema())
                        .addSchemas("ValidationErrorResponse", createValidationErrorResponseSchema()))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth").addList("csrfToken"));
    }

    /**
     * API 태그 순서 정의
     * Swagger UI에서 API 그룹 순서를 지정합니다.
     */
    private List<Tag> createApiTags() {
        return List.of(
                new Tag().name("Authentication API").description("인증 관련 API (회원가입, 로그인, 로그아웃, 토큰 갱신, 로그인 이력)"),
                new Tag().name("OAuth2 Authentication API").description("OAuth2 소셜 로그인 관련 API"),
                new Tag().name("CSRF API").description("CSRF 토큰 관리 API"),
                new Tag().name("Member Profile API").description("회원 프로필 관련 API"),
                new Tag().name("Medication Schedule API").description("복약 스케줄 관련 CRUD API"),
                new Tag().name("Medication Time API").description("복약 알림 시간 관련 CRUD API"),
                new Tag().name("Medication Record API").description("복약 기록 관련 CRUD API"),
                new Tag().name("Medication Stats API").description("복약 통계 관련 API"),
                new Tag().name("Medication Group API").description("복약 스케줄 그룹 관리 API"),
                new Tag().name("Side Effect API").description("사용자 부작용 기록 관련 API"),
                new Tag().name("Mood Diary API").description("마음 일기 관련 CRUD API"),
                new Tag().name("Notification API").description("푸시 알림 설정 및 디바이스 토큰 관리 API"),
                new Tag().name("Export API").description("데이터 내보내기 API (JSON, CSV)")
        );
    }

    @SuppressWarnings("rawtypes")
    private Schema createErrorResponseSchema() {
        return new ObjectSchema()
                .description("표준 에러 응답")
                .addProperty("code", new StringSchema().description("에러 코드").example("NOT_FOUND"))
                .addProperty(
                        "message", new StringSchema().description("에러 메시지").example("요청한 리소스를 찾을 수 없습니다"))
                .addProperty("path", new StringSchema().description("요청 경로").example("/medication-schedule/1"))
                .addProperty("violations", new StringSchema().nullable(true).description("유효성 검사 위반 정보 (일반 에러 시 null)"))
                .addProperty(
                        "timestamp",
                        new DateTimeSchema().description("에러 발생 시각 (ISO 8601)").example("2025-12-17T12:34:56.789"))
                .addProperty(
                        "traceId",
                        new StringSchema()
                                .description("요청 추적 ID (로그 상관관계 분석용)")
                                .example("550e8400-e29b-41d4-a716-446655440000"));
    }

    @SuppressWarnings("rawtypes")
    private Schema createValidationErrorResponseSchema() {
        return new ObjectSchema()
                .description("유효성 검사 실패 에러 응답")
                .addProperty("code", new StringSchema().description("에러 코드").example("BAD_REQUEST"))
                .addProperty(
                        "message", new StringSchema().description("에러 메시지").example("입력값이 유효하지 않습니다"))
                .addProperty("path", new StringSchema().description("요청 경로").example("/auth/signup"))
                .addProperty(
                        "violations",
                        new MapSchema()
                                .additionalProperties(new StringSchema())
                                .description("필드별 유효성 검사 에러 메시지")
                                .example("{\"loginId\": \"올바른 이메일 형식이어야 합니다\", \"nickName\": \"닉네임은 1~10자여야 합니다\"}"))
                .addProperty(
                        "timestamp",
                        new DateTimeSchema().description("에러 발생 시각 (ISO 8601)").example("2025-12-17T12:34:56.789"))
                .addProperty(
                        "traceId",
                        new StringSchema()
                                .description("요청 추적 ID (로그 상관관계 분석용)")
                                .example("550e8400-e29b-41d4-a716-446655440000"));
    }
}