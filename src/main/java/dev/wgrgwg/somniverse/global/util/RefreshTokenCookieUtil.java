package dev.wgrgwg.somniverse.global.util;

import dev.wgrgwg.somniverse.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieUtil {

    private static final String NAME = "refreshToken";
    private final AppProperties appProperties;

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(NAME, token)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(appProperties.getJwt().getRefreshTokenExpirationMs() / 1000)
            .sameSite("Strict")
            .build();
    }
}
