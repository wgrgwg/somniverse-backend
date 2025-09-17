package dev.wgrgwg.somniverse.security.oauth.handler;

import dev.wgrgwg.somniverse.config.AppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String ERROR_PARAM = "error";

    private final AppProperties appProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception)
        throws IOException, ServletException {

        String redirectUri = appProperties.getOauth().getAuthorizedRedirectUri();

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam(ERROR_PARAM, exception.getLocalizedMessage())
            .build().toUriString();

        log.warn("[OAuth 실패] 인증 실패 - 이유: {}", exception.getLocalizedMessage());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
