package dev.wgrgwg.somniverse.security.oauth.user;

import dev.wgrgwg.somniverse.global.exception.InternalServerException;
import dev.wgrgwg.somniverse.member.domain.Provider;
import dev.wgrgwg.somniverse.security.oauth.errorcode.OAuthErrorCode;
import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
        Map<String, Object> attributes) {
        Provider provider = Provider.fromString(registrationId);

        return switch (provider) {
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            case NAVER -> new NaverOAuth2UserInfo(attributes);
            default -> throw new InternalServerException(OAuthErrorCode.PROVIDER_NOT_FOUND);
        };
    }

}