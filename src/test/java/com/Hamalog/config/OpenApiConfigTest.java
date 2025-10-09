package com.Hamalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig Tests")
class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
        // Set default values for @Value fields
        ReflectionTestUtils.setField(openApiConfig, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(openApiConfig, "serverDescription", "Local Development Server");
    }

    @Test
    @DisplayName("Should create OpenAPI bean with correct configuration")
    void customOpenAPI_DefaultValues_CreatesCorrectOpenAPI() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // then
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getServers()).isNotNull();
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getSecurity()).isNotNull();
    }

    @Test
    @DisplayName("Should configure API info correctly")
    void customOpenAPI_ApiInfo_SetsCorrectInfo() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Info info = openAPI.getInfo();

        // then
        assertThat(info.getTitle()).isEqualTo("Hamalog API");
        assertThat(info.getDescription()).isEqualTo("복약 관리 시스템 API 문서입니다. JWT 토큰 기반 인증과 OAuth2(카카오) 로그인을 지원하며, 복약 스케줄링, 복약 기록, 부작용 추적 기능을 제공합니다.");
        assertThat(info.getVersion()).isEqualTo("v1.0.0");
    }

    @Test
    @DisplayName("Should configure contact information correctly")
    void customOpenAPI_ContactInfo_SetsCorrectContact() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Contact contact = openAPI.getInfo().getContact();

        // then
        assertThat(contact).isNotNull();
        assertThat(contact.getName()).isEqualTo("Hamalog Development Team");
        assertThat(contact.getEmail()).isEqualTo("support@hamalog.com");
        assertThat(contact.getUrl()).isEqualTo("https://github.com/hamalog");
    }

    @Test
    @DisplayName("Should configure license information correctly")
    void customOpenAPI_LicenseInfo_SetsCorrectLicense() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        License license = openAPI.getInfo().getLicense();

        // then
        assertThat(license).isNotNull();
        assertThat(license.getName()).isEqualTo("MIT License");
        assertThat(license.getUrl()).isEqualTo("https://opensource.org/licenses/MIT");
    }

    @Test
    @DisplayName("Should configure server information with default values")
    void customOpenAPI_ServerInfo_SetsCorrectServerWithDefaults() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        List<Server> servers = openAPI.getServers();

        // then
        assertThat(servers).hasSize(1);
        Server server = servers.get(0);
        assertThat(server.getUrl()).isEqualTo("http://localhost:8080");
        assertThat(server.getDescription()).isEqualTo("Local Development Server");
    }

    @Test
    @DisplayName("Should configure server information with custom values")
    void customOpenAPI_ServerInfo_SetsCorrectServerWithCustomValues() {
        // given
        String customUrl = "https://api.hamalog.com";
        String customDescription = "Production Server";
        ReflectionTestUtils.setField(openApiConfig, "serverUrl", customUrl);
        ReflectionTestUtils.setField(openApiConfig, "serverDescription", customDescription);

        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        List<Server> servers = openAPI.getServers();

        // then
        assertThat(servers).hasSize(1);
        Server server = servers.get(0);
        assertThat(server.getUrl()).isEqualTo(customUrl);
        assertThat(server.getDescription()).isEqualTo(customDescription);
    }

    @Test
    @DisplayName("Should configure JWT security scheme correctly")
    void customOpenAPI_SecurityScheme_SetsCorrectJWTScheme() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Components components = openAPI.getComponents();

        // then
        assertThat(components).isNotNull();
        Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();
        assertThat(securitySchemes).containsKey("bearerAuth");

        SecurityScheme bearerAuth = securitySchemes.get("bearerAuth");
        assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
        assertThat(bearerAuth.getBearerFormat()).isEqualTo("JWT");
        assertThat(bearerAuth.getDescription()).isEqualTo("JWT 토큰을 Authorization 헤더에 Bearer 형태로 전달합니다.");
    }

    @Test
    @DisplayName("Should configure security requirements correctly")
    void customOpenAPI_SecurityRequirements_SetsCorrectRequirements() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        List<SecurityRequirement> security = openAPI.getSecurity();

        // then
        assertThat(security).hasSize(1);
        SecurityRequirement securityRequirement = security.get(0);
        assertThat(securityRequirement).containsKey("bearerAuth");
        assertThat(securityRequirement.get("bearerAuth")).isEmpty(); // Empty list for bearer auth
    }

    @Test
    @DisplayName("Should handle empty server URL gracefully")
    void customOpenAPI_EmptyServerUrl_HandlesGracefully() {
        // given
        ReflectionTestUtils.setField(openApiConfig, "serverUrl", "");
        ReflectionTestUtils.setField(openApiConfig, "serverDescription", "");

        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        List<Server> servers = openAPI.getServers();

        // then
        assertThat(servers).hasSize(1);
        Server server = servers.get(0);
        assertThat(server.getUrl()).isEmpty();
        assertThat(server.getDescription()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null server values gracefully")
    void customOpenAPI_NullServerValues_HandlesGracefully() {
        // given
        ReflectionTestUtils.setField(openApiConfig, "serverUrl", null);
        ReflectionTestUtils.setField(openApiConfig, "serverDescription", null);

        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        List<Server> servers = openAPI.getServers();

        // then
        assertThat(servers).hasSize(1);
        Server server = servers.get(0);
        assertThat(server.getUrl()).isNull();
        assertThat(server.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should create immutable OpenAPI structure")
    void customOpenAPI_MultipleInvocations_CreatesNewInstances() {
        // when
        OpenAPI openAPI1 = openApiConfig.customOpenAPI();
        OpenAPI openAPI2 = openApiConfig.customOpenAPI();

        // then
        assertThat(openAPI1).isNotSameAs(openAPI2);
        assertThat(openAPI1.getInfo()).isNotSameAs(openAPI2.getInfo());
        assertThat(openAPI1.getServers()).isNotSameAs(openAPI2.getServers());
        
        // But content should be the same
        assertThat(openAPI1.getInfo().getTitle()).isEqualTo(openAPI2.getInfo().getTitle());
        assertThat(openAPI1.getServers().get(0).getUrl()).isEqualTo(openAPI2.getServers().get(0).getUrl());
    }

    @Test
    @DisplayName("Should configure components with security schemes only")
    void customOpenAPI_Components_ConfiguresSecuritySchemesOnly() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Components components = openAPI.getComponents();

        // then
        assertThat(components).isNotNull();
        assertThat(components.getSecuritySchemes()).hasSize(1);
        assertThat(components.getSchemas()).isNullOrEmpty();
        assertThat(components.getResponses()).isNullOrEmpty();
        assertThat(components.getParameters()).isNullOrEmpty();
        assertThat(components.getExamples()).isNullOrEmpty();
        assertThat(components.getRequestBodies()).isNullOrEmpty();
        assertThat(components.getHeaders()).isNullOrEmpty();
        assertThat(components.getLinks()).isNullOrEmpty();
        assertThat(components.getCallbacks()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should configure API with Korean description")
    void customOpenAPI_KoreanContent_HandlesCorrectly() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // then
        String description = openAPI.getInfo().getDescription();
        assertThat(description).contains("복약 관리");
        assertThat(description).contains("JWT 토큰");
        assertThat(description).contains("OAuth2");
        assertThat(description).contains("카카오");
        assertThat(description).contains("복약 스케줄링");
        assertThat(description).contains("부작용 추적");

        SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(securityScheme.getDescription()).contains("JWT 토큰을");
        assertThat(securityScheme.getDescription()).contains("Authorization 헤더에");
        assertThat(securityScheme.getDescription()).contains("Bearer 형태로");
    }

    @Test
    @DisplayName("Should have proper API versioning")
    void customOpenAPI_Versioning_SetsCorrectVersion() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // then
        assertThat(openAPI.getInfo().getVersion()).matches("v\\d+\\.\\d+\\.\\d+");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
    }

    @Test
    @DisplayName("Should maintain consistent structure across calls")
    void customOpenAPI_ConsistentStructure_MaintainsStructure() {
        // when
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // then - Verify complete structure
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isNotNull();
        assertThat(openAPI.getInfo().getDescription()).isNotNull();
        assertThat(openAPI.getInfo().getVersion()).isNotNull();
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getLicense()).isNotNull();
        
        assertThat(openAPI.getServers()).isNotEmpty();
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).isNotEmpty();
        assertThat(openAPI.getSecurity()).isNotEmpty();
    }
}