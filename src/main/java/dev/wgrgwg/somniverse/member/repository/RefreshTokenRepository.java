package dev.wgrgwg.somniverse.member.repository;

import dev.wgrgwg.somniverse.global.util.HashUtil;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;
    public static final String REFRESH_TOKEN_PREFIX = "RT:";

    @Value("${spring.jwt.refresh-token-expiration-ms}")
    private Long refreshTokenExpire;

    public void save(String refreshToken, String memberId) {
        redisTemplate.opsForValue().set(
            redisKey(refreshToken),
            memberId,
            Duration.ofMillis(refreshTokenExpire)
        );
    }

    public Optional<String> findMemberIdByToken(String refreshToken) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(refreshToken)));
    }

    public void delete(String refreshToken) {
        redisTemplate.delete(redisKey(refreshToken));
    }

    private String redisKey(String refreshToken) {
        return REFRESH_TOKEN_PREFIX + HashUtil.sha256(refreshToken);
    }
}
