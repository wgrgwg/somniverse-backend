package dev.wgrgwg.somniverse.security.jwt.dto;

public record ReissueResponse(
    String accessToken,
    String refreshToken
) {

}
