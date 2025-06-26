package dev.wgrgwg.somniverse.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.wgrgwg.somniverse.global.exception.CustomException;
import dev.wgrgwg.somniverse.member.domain.Member;
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
    @Mock
    private MemberService memberService;

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
        verify(refreshTokenRepository).save("refresh-token", "1");
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("토큰 재발급 성공 시 새로운 토큰 응답 반환")
    void reissue_success_shouldReturnNewTokenResponse() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String newRefreshToken = "new-refresh-token";
        TokenResponse newTokens = new TokenResponse("new-access-token", newRefreshToken);

        when(jwtProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(refreshTokenRepository.findMemberIdByToken(oldRefreshToken))
            .thenReturn(Optional.of("1"));
        when(memberService.findById(1L)).thenReturn(testMember);
        when(jwtProvider.generateToken(testMember)).thenReturn(newTokens);

        // when
        TokenResponse result = authService.reissue(oldRefreshToken);

        // then
        verify(jwtProvider).validateToken(oldRefreshToken);
        verify(refreshTokenRepository).findMemberIdByToken(oldRefreshToken);
        verify(memberService).findById(1L);
        verify(jwtProvider).generateToken(testMember);
        verify(refreshTokenRepository).delete(oldRefreshToken);
        verify(refreshTokenRepository).save(newRefreshToken, "1");

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
    @DisplayName("Redis에 없는 refresh token으로 재발급 시도하면 예외 발생")
    void reissue_whenTokenNotFoundInRedis_shouldThrowException() {
        // given
        String notFoundToken = "not-found-refresh-token";
        when(jwtProvider.validateToken(notFoundToken)).thenReturn(true);
        when(refreshTokenRepository.findMemberIdByToken(notFoundToken)).thenReturn(
            Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissue(notFoundToken))
            .isInstanceOf(CustomException.class)
            .hasMessage(MemberErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("로그아웃 성공 시 Redis에서 refresh token 삭제")
    void logout_success_shouldDeleteRefreshToken() {
        // given
        String refreshTokenValue = "refresh-token-to-delete";
        when(refreshTokenRepository.findMemberIdByToken(refreshTokenValue))
            .thenReturn(Optional.of("1"));

        // when
        authService.logout(refreshTokenValue);

        // then
        verify(refreshTokenRepository).findMemberIdByToken(refreshTokenValue);
        verify(refreshTokenRepository).delete(refreshTokenValue);
    }

    @Test
    @DisplayName("로그아웃 시 Redis에 토큰이 없으면 예외 발생")
    void logout_whenTokenNotFound_shouldThrowException() {
        // given
        String nonexistentToken = "nonexistent-token";
        when(refreshTokenRepository.findMemberIdByToken(nonexistentToken))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.logout(nonexistentToken))
            .isInstanceOf(CustomException.class)
            .hasMessage(MemberErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage());

        verify(refreshTokenRepository).findMemberIdByToken(nonexistentToken);
        verify(refreshTokenRepository, never()).delete(any());
    }
}