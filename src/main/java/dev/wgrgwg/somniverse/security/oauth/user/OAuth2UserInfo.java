package dev.wgrgwg.somniverse.security.oauth.user;

public interface OAuth2UserInfo {

    String getProviderId();

    String getEmail();

    String getName();
}
