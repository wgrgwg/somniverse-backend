package dev.wgrgwg.somniverse.member.dto.request;

public record ReissueRequest(
    String accessToken,
    String refreshToken
) {

}
