package dev.wgrgwg.somniverse.member.dto.response;

public record TokenResponse(
    String accessToken,
    String refreshToken
) {

}
