package dev.wgrgwg.somniverse.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.RefreshToken;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.request.LoginRequest;
import dev.wgrgwg.somniverse.member.dto.response.TokenResponse;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import dev.wgrgwg.somniverse.member.repository.RefreshTokenRepository;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private Member testMember;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setup() {
        testMember = Member.builder()
            .id(1L)
            .username("testuser")
            .email("test@email.com")
            .password("encoded-password")
            .role(Role.USER)
            .build();

        testUserDetails = new CustomUserDetails(testMember);
    }

    @Test
    @DisplayName("로그인 성공 시 토큰 응답 반환")
    void login_success_shouldReturnTokenResponse() {
        // given
        LoginRequest loginRequest = new LoginRequest("test@email.com", "password");
        TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token");
        Authentication authentication = new UsernamePasswordAuthenticationToken(testUserDetails,
            null, testUserDetails.getAuthorities());

        when(authenticationManager.authenticate(
            any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtProvider.generateToken(testMember)).thenReturn(tokenResponse);

        // when
        TokenResponse result = authService.login(loginRequest);

        // then
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtProvider).generateToken(testMember);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("토큰 재발급 성공 시 새로운 토큰 응답 반환")
    void reissue_success_shouldReturnNewTokenResponse() {
        // given
        String oldRefreshTokenValue = "old-refresh-token";
        RefreshToken refreshTokenFromDB = RefreshToken.builder()
            .member(testMember)
            .value(oldRefreshTokenValue)
            .build();
        TokenResponse newTokens = new TokenResponse("new-access-token", "new-refresh-token");

        when(jwtProvider.validateToken(oldRefreshTokenValue)).thenReturn(true);
        when(refreshTokenRepository.findByValue(oldRefreshTokenValue)).thenReturn(
            Optional.of(refreshTokenFromDB));
        when(jwtProvider.generateToken(testMember)).thenReturn(newTokens);

        // when
        TokenResponse result = authService.reissue(oldRefreshTokenValue);

        // then
        verify(jwtProvider).validateToken(oldRefreshTokenValue);
        verify(refreshTokenRepository).findByValue(oldRefreshTokenValue);
        verify(jwtProvider).generateToken(testMember);
        assertThat(refreshTokenFromDB.getValue()).isEqualTo("new-refresh-token"); // 토큰 값 업데이트 확인
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("유효하지 않은 refresh token으로 토큰 재발급 시도하면 예외 발생")
    void reissue_withInvalidToken_shouldThrowException() {
        // given
        String invalidRefreshToken = "invalid-refresh-token";
        when(jwtProvider.validateToken(invalidRefreshToken)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(invalidRefreshToken))
            .isInstanceOf(CustomException.class)
            .hasMessage(MemberErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("DB에 없는 refresh token으로 재발급 시도하면 예외 발생")
    void reissue_whenTokenNotFoundInDB_shouldThrowException() {
        // given
        String refreshTokenNotInDB = "not-found-refresh-token";
        when(jwtProvider.validateToken(refreshTokenNotInDB)).thenReturn(true);
        when(refreshTokenRepository.findByValue(refreshTokenNotInDB)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshTokenNotInDB))
            .isInstanceOf(CustomException.class)
            .hasMessage(MemberErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("로그아웃 성공 시 DB에서 refresh token 삭제")
    void logout_success_shouldDeleteRefreshToken() {
        // given
        String refreshTokenValue = "refresh-token-to-delete";
        RefreshToken refreshToken = RefreshToken.builder()
            .member(testMember)
            .value(refreshTokenValue)
            .build();
        when(refreshTokenRepository.findByValue(refreshTokenValue)).thenReturn(
            Optional.of(refreshToken));

        // when
        authService.logout(refreshTokenValue);

        // then
        verify(refreshTokenRepository).findByValue(refreshTokenValue);
        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    @DisplayName("로그아웃 시 토큰이 DB에 없어도 오류 발생하지 않음")
    void logout_whenTokenNotFound_shouldNotThrowException() {
        // given
        String refreshTokenNotInDB = "not-found-refresh-token";
        when(refreshTokenRepository.findByValue(refreshTokenNotInDB)).thenReturn(Optional.empty());

        // when
        authService.logout(refreshTokenNotInDB);

        // then
        verify(refreshTokenRepository).findByValue(refreshTokenNotInDB);
        verify(refreshTokenRepository, never()).delete(any());
    }
}