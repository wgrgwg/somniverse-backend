package dev.wgrgwg.somniverse.global.util;

import dev.wgrgwg.somniverse.security.jwt.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieUtil {

    private static final String NAME = "refreshToken";
    private final JwtProperties jwtProperties;

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(NAME, token)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(jwtProperties.getRefreshTokenExpirationMs() / 1000)
            .sameSite("Strict")
            .build();
    }
}
