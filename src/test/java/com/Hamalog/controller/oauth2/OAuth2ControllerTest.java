/*
package com.Hamalog.controller.oauth2;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2 Controller Tests")
class OAuth2ControllerTest {

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private OAuth2Controller oAuth2Controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(oAuth2Controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should redirect to Kakao authorization server successfully")
    void kakaoAuth_ValidConfiguration_ShouldRedirectToKakao() throws Exception {
        // given
        ClientRegistration kakaoRegistration = createTestClientRegistration();
        when(clientRegistrationRepository.findByRegistrationId("kakao")).thenReturn(kakaoRegistration);

        // when & then
        mockMvc.perform(get("/api/v1/oauth2/auth/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String location = result.getResponse().getHeader("Location");
                    assertThat(location).contains("https://kauth.kakao.com/oauth/authorize");
                    assertThat(location).contains("client_id=test-client-id");
                    assertThat(location).contains("response_type=code");
                    assertThat(location).contains("scope=profile_nickname");
                    assertThat(location).contains("redirect_uri=http://localhost:8080/api/auth/kakao/callback");
                    assertThat(location).contains("state="); // UUID state parameter should be present
                });

        verify(clientRegistrationRepository).findByRegistrationId("kakao");
    }

    @Test
    @DisplayName("Should return 500 when Kakao client registration is not found")
    void kakaoAuth_NoClientRegistration_ShouldReturn500() throws Exception {
        // given
        when(clientRegistrationRepository.findByRegistrationId("kakao")).thenReturn(null);

        // when & then
        mockMvc.perform(get("/api/v1/oauth2/auth/kakao"))
                .andExpect(status().isInternalServerError())
                .andExpect(result -> {
                    MockHttpServletResponse response = result.getResponse();
                    assertThat(response.getErrorMessage()).contains("카카오 OAuth2 설정이 없습니다.");
                });

        verify(clientRegistrationRepository).findByRegistrationId("kakao");
    }

    @Test
    @DisplayName("Should return 500 when exception occurs during authorization URL building")
    void kakaoAuth_ExceptionDuringProcessing_ShouldReturn500() throws Exception {
        // given
        when(clientRegistrationRepository.findByRegistrationId("kakao"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // when & then
        mockMvc.perform(get("/api/v1/oauth2/auth/kakao"))
                .andExpect(status().isInternalServerError())
                .andExpect(result -> {
                    MockHttpServletResponse response = result.getResponse();
                    assertThat(response.getErrorMessage()).contains("OAuth2 인증 초기화 중 오류가 발생했습니다");
                    assertThat(response.getErrorMessage()).contains("Database connection failed");
                });
    }

    @Test
    @DisplayName("Should handle Kakao callback successfully and return JWT token")
    void handleKakaoCallback_ValidCode_ShouldReturnLoginResponse() throws Exception {
        // given
        String authorizationCode = "valid-auth-code-12345";
        LoginResponse loginResponse = new LoginResponse("jwt-token-123");

        when(authService.processOAuth2Callback(authorizationCode)).thenReturn(loginResponse);

        // when & then
        mockMvc.perform(post("/api/auth/kakao/callback")
                .param("code", authorizationCode)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-123"));

        verify(authService).processOAuth2Callback(authorizationCode);
    }

    @Test
    @DisplayName("Should return 400 when authorization code parameter is missing")
    void handleKakaoCallback_MissingCodeParameter_ShouldReturn400() throws Exception {
        // when & then
        mockMvc.perform(post("/api/auth/kakao/callback")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Should return 500 when OAuth2 processing fails")
    void handleKakaoCallback_OAuth2ProcessingFails_ShouldReturn500() throws Exception {
        // given
        String authorizationCode = "invalid-auth-code";
        when(authService.processOAuth2Callback(authorizationCode))
                .thenThrow(new RuntimeException("OAuth2 token exchange failed"));

        // when & then
        mockMvc.perform(post("/api/auth/kakao/callback")
                .param("code", authorizationCode)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(authService).processOAuth2Callback(authorizationCode);
    }

    @Test
    @DisplayName("Should handle empty authorization code gracefully")
    void handleKakaoCallback_EmptyCode_ShouldCallService() throws Exception {
        // given
        String emptyCode = "";
        when(authService.processOAuth2Callback(emptyCode))
                .thenThrow(new IllegalArgumentException("Authorization code cannot be empty"));

        // when & then
        mockMvc.perform(post("/api/auth/kakao/callback")
                .param("code", emptyCode)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(authService).processOAuth2Callback(emptyCode);
    }

    @Test
    @DisplayName("Should handle long authorization code properly")
    void handleKakaoCallback_LongAuthCode_ShouldTruncateInLog() throws Exception {
        // given
        String longCode = "a".repeat(100); // 100 character code
        LoginResponse loginResponse = new LoginResponse("jwt-token-456");

        when(authService.processOAuth2Callback(longCode)).thenReturn(loginResponse);

        // when & then
        mockMvc.perform(post("/api/auth/kakao/callback")
                .param("code", longCode)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-456"));

        verify(authService).processOAuth2Callback(longCode);
    }

    @Test
    @DisplayName("Should handle authentication service returning null")
    void handleKakaoCallback_ServiceReturnsNull_ShouldHandleGracefully() throws Exception {
        // given
        String authorizationCode = "valid-code";
        when(authService.processOAuth2Callback(authorizationCode)).thenReturn(null);

        // when & then - Spring handles null responses gracefully with 200 status and null body
        mockMvc.perform(post("/api/auth/kakao/callback")
                .param("code", authorizationCode)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should verify authorization URL contains all required OAuth2 parameters")
    void kakaoAuth_ValidConfiguration_ShouldContainAllRequiredParameters() throws Exception {
        // given
        ClientRegistration kakaoRegistration = createTestClientRegistration();
        when(clientRegistrationRepository.findByRegistrationId("kakao")).thenReturn(kakaoRegistration);

        // when & then
        mockMvc.perform(get("/api/v1/oauth2/auth/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String location = result.getResponse().getHeader("Location");

                    // Verify all required OAuth2 parameters are present
                    assertThat(location).contains("response_type=code");
                    assertThat(location).contains("client_id=");
                    assertThat(location).contains("scope=");
                    assertThat(location).contains("state=");
                    assertThat(location).contains("redirect_uri=");

                    // Verify the state parameter is a valid UUID format
                    String[] params = location.split("[&?]");
                    String stateParam = null;
                    for (String param : params) {
                        if (param.startsWith("state=")) {
                            stateParam = param.substring("state=".length());
                            break;
                        }
                    }
                    assertThat(stateParam).isNotNull();
                    assertThat(stateParam).hasSize(36); // UUID format length with hyphens
                });
    }

    */
/**
     * Helper method to create a valid test ClientRegistration for Kakao
     *//*

    private ClientRegistration createTestClientRegistration() {
        return ClientRegistration.withRegistrationId("kakao")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/api/auth/kakao/callback")
                .scope("profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
    }
}*/
