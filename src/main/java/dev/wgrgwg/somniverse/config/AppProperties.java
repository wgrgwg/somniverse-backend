package dev.wgrgwg.somniverse.config;

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
}
