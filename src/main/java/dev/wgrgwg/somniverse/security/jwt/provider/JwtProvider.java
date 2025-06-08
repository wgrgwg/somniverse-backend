package dev.wgrgwg.somniverse.security.jwt.provider;

import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.security.jwt.dto.TokenDto;
import dev.wgrgwg.somniverse.security.jwt.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    public TokenDto generateToken(Member member) {
        String subject = member.getId().toString();

        String role = member.getRole().name();

        String accessToken = createJwt(subject, "access", role,
            jwtProperties.getAccessTokenExpirationMs());

        String refreshToken = createJwt(subject, "refresh", role,
            jwtProperties.getRefreshTokenExpirationMs());

        return new TokenDto(accessToken, refreshToken);
    }

    private String createJwt(String subject, String category, String role, Long expiredMs) {
        return Jwts.builder()
            .subject(subject)
            .claim("category", category)
            .claim("role", role)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(
                new Date(System.currentTimeMillis() + expiredMs))
            .signWith(jwtProperties.getSecretKey())
            .compact();
    }
}
