package dev.wgrgwg.somniverse.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wgrgwg.somniverse.config.AppProperties;
import dev.wgrgwg.somniverse.global.errorcode.CommonErrorCode;
import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.global.idempotency.filter.IdempotencyFilter;
import dev.wgrgwg.somniverse.global.idempotency.store.IdempotencyRepository;
import dev.wgrgwg.somniverse.global.ratelimit.filter.RateLimitFilter;
import dev.wgrgwg.somniverse.global.util.RefreshTokenCookieUtil;
import dev.wgrgwg.somniverse.member.dto.request.LoginRequest;
import dev.wgrgwg.somniverse.member.dto.response.TokenResponse;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.repository.AccessTokenBlackListRepository;
import dev.wgrgwg.somniverse.member.service.AuthService;
import dev.wgrgwg.somniverse.security.config.SecurityConfig;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
import dev.wgrgwg.somniverse.security.oauth.handler.OAuth2AuthenticationFailureHandler;
import dev.wgrgwg.somniverse.security.oauth.handler.OAuth2AuthenticationSuccessHandler;
import dev.wgrgwg.somniverse.security.oauth.service.CustomOAuth2UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(AppProperties.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenCookieUtil refreshTokenCookieUtil;

    @MockitoBean
    private AccessTokenBlackListRepository blackListRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private IdempotencyRepository idempotencyRepository;

    @MockitoBean
    private IdempotencyFilter idempotencyFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void passThroughFilters() throws Exception {
        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(idempotencyFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());
    }

    @Nested
    @DisplayName("로그인 api 테스트")
    class LoginApiTests {

        @Test
        @DisplayName("로그인 성공 시 200 OK, token 반환")
        void login_success_test() throws Exception {
            // given
            LoginRequest loginRequest = new LoginRequest("test@email.com", "password123!");
            TokenResponse tokenResponse = new TokenResponse("test-access-token",
                "test-refresh-token");
            ResponseCookie responseCookie = ResponseCookie.from("refreshToken",
                    "test-refresh-token")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);
            when(refreshTokenCookieUtil.createRefreshTokenCookie(
                tokenResponse.refreshToken())).thenReturn(responseCookie);

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/auth/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, responseCookie.toString()))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("test-access-token"));
        }

        @Test
        @DisplayName("올바르지 않은 아이디/비밀번호로 로그인 시 401 UNAUTHORIZED, 에러 응답 반환")
        void login_fail_withBadCredentials_test() throws Exception {
            // given
            LoginRequest loginRequest = new LoginRequest("test@email.com", "wrong-password123!");
            when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException(
                    MemberErrorCode.INVALID_CREDENTIALS.getMessage()));

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/auth/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

            // then
            resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                    MemberErrorCode.INVALID_CREDENTIALS.getMessage()));
        }
    }

    @Nested
    @DisplayName("토큰 재발급 api 테스트")
    class ReissueTest {

        @Test
        @DisplayName("토큰 재발급 성공 시 200 OK, token 반환")
        void reissue_success_test() throws Exception {
            // given
            String oldRefreshToken = "old-refresh-token";
            Cookie refreshTokenCookie = new Cookie("refreshToken", oldRefreshToken);
            TokenResponse newTokens = new TokenResponse("new-access-token", "new-refresh-token");
            ResponseCookie newResponseCookie = ResponseCookie.from("refreshToken",
                "new-refresh-token").build();

            when(authService.reissue(oldRefreshToken)).thenReturn(newTokens);
            when(refreshTokenCookieUtil.createRefreshTokenCookie(
                newTokens.refreshToken())).thenReturn(newResponseCookie);

            // when
            ResultActions resultActions = mockMvc.perform(put("/api/auth/tokens")
                .cookie(refreshTokenCookie));

            // then
            resultActions.andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }

        @Test
        @DisplayName("refreshToken 쿠키가 없을 시 실패, 400 BAD REQUEST 반환")
        void reissue_fail_whenRefreshTokenCookieIsMissing_test() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(put("/api/auth/tokens"));

            // then
            resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                    .value(CommonErrorCode.MISSING_COOKIE.getMessage()))
                .andExpect(jsonPath("$.errorCode")
                    .value(CommonErrorCode.MISSING_COOKIE.getCode()));
        }

        @Test
        @DisplayName("유효하지 않은 refreshToken으로 실패 시 403 UNAUTHORIZED, 에러 응답 반환")
        void reissue_fail_withInvalidToken_test() throws Exception {
            // given
            String invalidRefreshToken = "invalid-refresh-token";
            Cookie refreshTokenCookie = new Cookie("refreshToken", invalidRefreshToken);
            when(authService.reissue(invalidRefreshToken))
                .thenThrow(new CustomException(MemberErrorCode.INVALID_REFRESH_TOKEN));

            // when
            ResultActions resultActions = mockMvc.perform(
                put("/api/auth/tokens").cookie(refreshTokenCookie));

            // then
            resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                    .value(MemberErrorCode.INVALID_REFRESH_TOKEN.getMessage()))
                .andExpect(jsonPath("$.errorCode")
                    .value(MemberErrorCode.INVALID_REFRESH_TOKEN.getCode()));
        }
    }

    @Nested
    @DisplayName("로그아웃 API 테스트")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 성공 시 204 NO CONTENT 반환")
        void logout_success_test() throws Exception {
            // given
            String accessToken = "Bearer access-token";
            String refreshToken = "refresh-token-to-delete";
            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);

            doNothing().when(authService).logout(accessToken, refreshToken);

            // when
            ResultActions resultActions = mockMvc.perform(delete("/api/auth/tokens")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .cookie(refreshTokenCookie));

            // then
            resultActions.andExpect(status().isNoContent());
            verify(authService, times(1)).logout(accessToken, refreshToken);
        }

        @Test
        @DisplayName("refreshToken 쿠키가 없을 시 실패, 400 BAD REQUEST 반환")
        void logout_fail_whenRefreshTokenCookieIsMissing_test() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(delete("/api/auth/tokens")
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-access-token"));

            // then
            resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                    .value(CommonErrorCode.MISSING_COOKIE.getMessage()))
                .andExpect(jsonPath("$.errorCode")
                    .value(CommonErrorCode.MISSING_COOKIE.getCode()));
        }

        @Test
        @DisplayName("블랙리스트에 있는 accessToken으로 접근 시 401 반환")
        void accessToken_inBlackList_shouldReturn401() throws Exception {
            // given
            String token = "blacklisted-access-token";

            when(blackListRepository.exists(token)).thenReturn(true);

            // when & then
            mockMvc.perform(get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        }
    }
}
