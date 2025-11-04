package dev.wgrgwg.somniverse.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {

    @Valid
    private final Jwt jwt = new Jwt();

    @Valid
    private final Oauth oauth = new Oauth();

    @Valid
    private final Cors cors = new Cors();

    @Valid
    private final Idempotency idempotency = new Idempotency();

    @Valid
    private final RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Jwt {

        @NotBlank
        private String secret;

        @NotNull
        @Positive
        private Long accessTokenExpirationMs;

        @NotNull
        @Positive
        private Long refreshTokenExpirationMs;
    }

    @Getter
    @Setter
    public static class Oauth {

        @NotBlank
        private String authorizedRedirectUri;
    }

    @Getter
    @Setter
    public static class Cors {

        @NotEmpty
        private List<String> allowedOrigins;
    }

    @Getter
    @Setter
    public static class Idempotency {

        @Positive
        private int ttlSeconds;

        @Positive
        private int inProgressTtl;

        @Positive
        private int retryAfterSeconds;

        @NotEmpty
        private List<String> includePaths;
    }

    @Getter
    @Setter
    public static class RateLimit {

        private boolean enabled;
        private boolean addHeaders;

        @NotEmpty
        private List<String> whitelistPaths;

        @NotEmpty
        @Valid
        private List<Policy> policies;

        @Getter
        @Setter
        public static class Policy {

            @NotBlank
            private String name;

            @NotEmpty
            private List<String> paths;

            @NotEmpty
            private List<String> methods;

            @NotBlank
            private String keyStrategy;

            @NotEmpty
            @Valid
            private List<Limit> limits;
        }

        @Getter
        @Setter
        public static class Limit {

            @Positive
            private int capacity;

            @NotNull
            private Duration refill;
        }
    }
}
