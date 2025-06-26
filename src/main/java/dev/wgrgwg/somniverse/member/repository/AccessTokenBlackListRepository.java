package dev.wgrgwg.somniverse.member.repository;

import dev.wgrgwg.somniverse.global.util.HashUtil;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccessTokenBlackListRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    public static final String BLACKLIST_PREFIX = "BL:";
    public static final String BL_VALUE = "logout";

    public void save(String accessToken, long remainingExpirationMillis) {
        String hashedToken = HashUtil.sha256(accessToken);
        String key = BLACKLIST_PREFIX + hashedToken;

        redisTemplate.opsForValue()
            .set(key, BL_VALUE, Duration.ofMillis(remainingExpirationMillis));
    }

    public boolean exists(String accessToken) {
        String hashedToken = HashUtil.sha256(accessToken);
        String key = BLACKLIST_PREFIX + hashedToken;

        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
