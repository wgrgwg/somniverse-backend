package dev.wgrgwg.somniverse.security.jwt.dto;

public record TokenDto(
    String accessToken,
    String refreshToken
) {

}
