package dev.wgrgwg.somniverse.config;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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

        private SecretKey secretKey;

        @PostConstruct
        private void init() {
            this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
        }
    }

    @Getter
    @Setter
    public static class Oauth {
        private String authorizedRedirectUri;
    }
}
