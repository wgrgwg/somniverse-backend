package dev.wgrgwg.somniverse;

import dev.wgrgwg.somniverse.global.ratelimit.config.RateLimitConfig;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SomniverseApplicationTests {

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private LettuceBasedProxyManager<String> proxyManager;

    @MockitoBean
    private StatefulRedisConnection<String, String> rateLimitRedisConn;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void contextLoads() {
    }

}
