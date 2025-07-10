package dev.wgrgwg.somniverse.security.oauth.handler;

import dev.wgrgwg.somniverse.config.AppProperties;
import dev.wgrgwg.somniverse.global.util.RefreshTokenCookieUtil;
import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.dto.response.TokenResponse;
import dev.wgrgwg.somniverse.member.repository.RefreshTokenRepository;
import dev.wgrgwg.somniverse.security.jwt.provider.JwtProvider;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String ACCESS_TOKEN_PARAM = "accessToken";

    private final JwtProvider jwtProvider;
    private final AppProperties appProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCookieUtil refreshTokenCookieUtil;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {
        CustomUserDetails oAuth2User = (CustomUserDetails) authentication.getPrincipal();
        Member member = oAuth2User.getMember();

        TokenResponse tokenResponse = jwtProvider.generateToken(member);
        log.info("[OAuth Success] JWT 발급 완료 - Member ID: {}, AccessToken: {}****",
            member.getId(), tokenResponse.accessToken().substring(0, 8));

        saveRefreshToken(member.getId(), tokenResponse.refreshToken());

        addRefreshTokenCookie(response, tokenResponse.refreshToken());

        clearAuthenticationAttributes(request);

        String targetUrl = getTargetUrl(tokenResponse.accessToken());
        log.info("[OAuth Success] 최종 리다이렉트 URL: {}", targetUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private void saveRefreshToken(Long memberId, String refreshToken) {
        refreshTokenRepository.save(refreshToken, memberId.toString());
        log.info("[OAuth] Redis에 Refresh Token 저장 완료 - Member ID: {}", memberId);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie refreshTokenCookie = refreshTokenCookieUtil.createRefreshTokenCookie(
            refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        log.info("[OAuth] Refresh Token 쿠키 추가 완료 (HttpOnly)");
    }

    private String getTargetUrl(String accessToken) {
        String redirectUri = appProperties.getOauth().getAuthorizedRedirectUri();

        return UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam(ACCESS_TOKEN_PARAM, accessToken)
            .build()
            .toUriString();
    }
}
