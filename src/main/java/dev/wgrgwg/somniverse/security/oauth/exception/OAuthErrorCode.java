package dev.wgrgwg.somniverse.security.oauth.exception;

import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OAuthErrorCode implements ErrorCode {

    PROVIDER_NOT_FOUND("OAUTH_001", "존재하지 않는 OAuth 플랫폼입니다"),
    INVALID_USERNAME("OAUTH_002", "사용자명 생성 오류입니다");

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    OAuthErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
