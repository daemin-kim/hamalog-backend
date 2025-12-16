package com.Hamalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
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
                        .description("복약 관리 시스템 API 문서입니다. JWT 토큰 기반 인증과 OAuth2(카카오) 로그인을 지원하며, 복약 스케줄링, 복약 기록, 부작용 추적 기능을 제공합니다.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Hamalog Development Team")
                                .email("support@hamalog.com")
                                .url("https://github.com/hamalog"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(serverUrl).description(serverDescription)))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 Authorization 헤더에 Bearer 형태로 전달합니다.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}