package dev.wgrgwg.somniverse.security.jwt.provider;

import dev.wgrgwg.somniverse.member.domain.Member;
import dev.wgrgwg.somniverse.member.domain.Role;
import dev.wgrgwg.somniverse.member.dto.response.TokenResponse;
import dev.wgrgwg.somniverse.security.jwt.properties.JwtProperties;
import dev.wgrgwg.somniverse.security.userdetails.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    public TokenResponse generateToken(Member member) {
        String subject = member.getId().toString();

        String role = member.getRole().name();

        String accessToken = createJwt(subject, "access", role,
            jwtProperties.getAccessTokenExpirationMs());

        String refreshToken = createJwt(subject, "refresh", role,
            jwtProperties.getRefreshTokenExpirationMs());

        return new TokenResponse(accessToken, refreshToken);
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

    public boolean validateToken(String token) {
        try {
            return !getClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        Collection<? extends GrantedAuthority> authorities =
            Arrays.stream(claims.get("role").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();

        Long memberId = Long.parseLong(claims.getSubject());
        Role role = Role.valueOf(claims.get("role").toString());

        Member member = Member.builder().id(memberId).role(role).build();
        CustomUserDetails userDetails = new CustomUserDetails(member);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(jwtProperties.getSecretKey()).build()
            .parseSignedClaims(token).getPayload();
    }
}
