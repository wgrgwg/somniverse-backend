package dev.wgrgwg.somniverse.security.jwt.dto;

public record ReissueRequest(
    String accessToken,
    String refreshToken
) {

}
