package dev.wgrgwg.somniverse.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Jwt jwt = new Jwt();

    private final Oauth oauth = new Oauth();

    private final Cors cors = new Cors();

    private final Idempotency idempotency = new Idempotency();

    @Getter
    @Setter
    public static class Jwt {

        private String secret;
        private Long accessTokenExpirationMs;
        private Long refreshTokenExpirationMs;
    }

    @Getter
    @Setter
    public static class Oauth {

        private String authorizedRedirectUri;
    }

    @Getter
    @Setter
    public static class Cors {

        private List<String> allowedOrigins;
    }

    @Getter
    @Setter
    public static class Idempotency {

        private int ttlSeconds;
        private int inProgressTtl;
        private int retryAfterSeconds;
        private List<String> includePaths;
    }
}
