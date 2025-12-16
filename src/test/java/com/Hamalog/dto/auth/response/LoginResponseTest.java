package com.Hamalog.dto.auth.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoginResponse DTO Tests")
class LoginResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should create valid LoginResponse with token")
    void constructor_WithValidToken_ShouldCreateResponse() {
        // given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTY5ODM2MDAwMH0.signature";

        // when
        LoginResponse loginResponse = new LoginResponse(token);

        // then
        assertThat(loginResponse.token()).isEqualTo(token);
    }

    @Test
    @DisplayName("Should handle null token")
    void constructor_WithNullToken_ShouldCreateResponse() {
        // given & when
        LoginResponse loginResponse = new LoginResponse(null);

        // then
        assertThat(loginResponse.token()).isNull();
    }

    @Test
    @DisplayName("Should handle empty token")
    void constructor_WithEmptyToken_ShouldCreateResponse() {
        // given
        String emptyToken = "";

        // when
        LoginResponse loginResponse = new LoginResponse(emptyToken);

        // then
        assertThat(loginResponse.token()).isEmpty();
    }

    @Test
    @DisplayName("Should maintain record equality and hashCode behavior")
    void record_ShouldMaintainEqualityAndHashCode() {
        // given
        String token = "sample-jwt-token";
        LoginResponse response1 = new LoginResponse(token);
        LoginResponse response2 = new LoginResponse(token);
        LoginResponse response3 = new LoginResponse("different-token");

        // then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.hashCode()).isNotEqualTo(response3.hashCode());
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void toString_ShouldContainTokenInformation() {
        // given
        String token = "sample-jwt-token";
        LoginResponse loginResponse = new LoginResponse(token);

        // when
        String result = loginResponse.toString();

        // then
        assertThat(result).contains("LoginResponse");
        assertThat(result).contains("token=sample-jwt-token");
    }

    @Test
    @DisplayName("Should serialize to JSON correctly")
    void serialization_ToJson_ShouldSerializeCorrectly() throws Exception {
        // given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTY5ODM2MDAwMH0.signature";
        LoginResponse loginResponse = new LoginResponse(token);

        // when
        String json = objectMapper.writeValueAsString(loginResponse);

        // then
        assertThat(json).contains("\"access_token\":");
        assertThat(json).contains(token);
        assertThat(json).contains("\"token_type\":");
        assertThat(json).contains("\"expires_in\":");
    }

    @Test
    @DisplayName("Should deserialize from JSON correctly")
    void deserialization_FromJson_ShouldDeserializeCorrectly() throws Exception {
        // given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTY5ODM2MDAwMH0.signature";
        String json = "{\"access_token\":\"" + token + "\",\"refresh_token\":null,\"expires_in\":3600,\"token_type\":\"Bearer\"}";

        // when
        LoginResponse loginResponse = objectMapper.readValue(json, LoginResponse.class);

        // then
        assertThat(loginResponse.token()).isEqualTo(token);
        assertThat(loginResponse.tokenType()).isEqualTo("Bearer");
        assertThat(loginResponse.expiresIn()).isEqualTo(3600);
    }

    @Test
    @DisplayName("Should handle serialization of null token")
    void serialization_WithNullToken_ShouldHandleCorrectly() throws Exception {
        // given
        LoginResponse loginResponse = new LoginResponse(null);

        // when
        String json = objectMapper.writeValueAsString(loginResponse);

        // then
        assertThat(json).contains("\"access_token\":null");
        assertThat(json).contains("\"token_type\":\"Bearer\"");
    }

    @Test
    @DisplayName("Should handle deserialization of null token")
    void deserialization_WithNullToken_ShouldHandleCorrectly() throws Exception {
        // given
        String json = "{\"access_token\":null,\"refresh_token\":null,\"expires_in\":3600,\"token_type\":\"Bearer\"}";

        // when
        LoginResponse loginResponse = objectMapper.readValue(json, LoginResponse.class);

        // then
        assertThat(loginResponse.token()).isNull();
        assertThat(loginResponse.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Should handle very long JWT tokens")
    void constructor_WithVeryLongToken_ShouldHandleCorrectly() {
        // given
        StringBuilder longToken = new StringBuilder("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.");
        longToken.append("eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTY5ODM2MDAwMCwicm9sZXMiOlsiVVNFUiIsIkFETUlOIl0sImRhdGEiOiI=".repeat(10));
        longToken.append(".signature");

        // when
        LoginResponse loginResponse = new LoginResponse(longToken.toString());

        // then
        assertThat(loginResponse.token()).isEqualTo(longToken.toString());
        assertThat(loginResponse.token().length()).isGreaterThan(500);
    }

    @Test
    @DisplayName("Should handle JWT token with special characters")
    void constructor_WithSpecialCharactersInToken_ShouldHandleCorrectly() {
        // given
        String tokenWithSpecialChars = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0K3VzZXIrMTIzIiwiZXhwIjoxNjk4MzYwMDAwfQ.special-signature_123+abc/def=";

        // when
        LoginResponse loginResponse = new LoginResponse(tokenWithSpecialChars);

        // then
        assertThat(loginResponse.token()).isEqualTo(tokenWithSpecialChars);
    }

    @Test
    @DisplayName("Should support method chaining in fluent style")
    void constructor_ShouldSupportFluentUsage() {
        // given & when
        String token = new LoginResponse("test-token").token();

        // then
        assertThat(token).isEqualTo("test-token");
    }

    @Test
    @DisplayName("Should be immutable after creation")
    void record_ShouldBeImmutableAfterCreation() {
        // given
        String originalToken = "original-token";
        LoginResponse loginResponse = new LoginResponse(originalToken);

        // when - record fields are final, so they can't be modified

        // then
        assertThat(loginResponse.token()).isEqualTo(originalToken);
    }

    @Test
    @DisplayName("Should handle Unicode characters in token")
    void constructor_WithUnicodeToken_ShouldHandleCorrectly() {
        // given
        String unicodeToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiLtZJ_smpzsnpAiLCJleHAiOjE2OTgzNjAwMDB9.signature-한글-test";

        // when
        LoginResponse loginResponse = new LoginResponse(unicodeToken);

        // then
        assertThat(loginResponse.token()).isEqualTo(unicodeToken);
    }
}