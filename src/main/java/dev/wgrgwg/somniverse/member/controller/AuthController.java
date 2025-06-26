package dev.wgrgwg.somniverse.member.controller;

import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import dev.wgrgwg.somniverse.global.util.RefreshTokenCookieUtil;
import dev.wgrgwg.somniverse.member.dto.request.LoginRequest;
import dev.wgrgwg.somniverse.member.dto.response.AccessTokenResponse;
import dev.wgrgwg.somniverse.member.dto.response.TokenResponse;
import dev.wgrgwg.somniverse.member.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth/tokens")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieUtil refreshTokenCookieUtil;

    @PostMapping
    public ResponseEntity<ApiResponseDto<AccessTokenResponse>> login(
        @Valid @RequestBody LoginRequest loginRequest) {
        TokenResponse tokenResponse = authService.login(loginRequest);

        ResponseCookie refreshTokenCookie = refreshTokenCookieUtil.createRefreshTokenCookie(
            tokenResponse.refreshToken());

        return ResponseEntity
            .status(HttpStatus.OK)
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(ApiResponseDto.success(new AccessTokenResponse(tokenResponse.accessToken())));
    }

    @PutMapping
    public ResponseEntity<ApiResponseDto<AccessTokenResponse>> reissue(
        @CookieValue("refreshToken") String refreshToken) {
        TokenResponse tokenResponse = authService.reissue(refreshToken);

        ResponseCookie refreshTokenCookie = refreshTokenCookieUtil.createRefreshTokenCookie(
            tokenResponse.refreshToken());

        return ResponseEntity
            .status(HttpStatus.OK)
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(ApiResponseDto.success(new AccessTokenResponse(tokenResponse.accessToken())));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponseDto<Void>> logout(
        @RequestHeader("Authorization") String accessToken,
        @CookieValue("refreshToken") String refreshToken) {
        authService.logout(accessToken, refreshToken);

        return ResponseEntity.
            status(HttpStatus.NO_CONTENT)
            .build();
    }
}
