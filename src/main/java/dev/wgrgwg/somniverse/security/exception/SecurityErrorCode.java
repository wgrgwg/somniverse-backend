package dev.wgrgwg.somniverse.security.exception;

import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SecurityErrorCode implements ErrorCode {
    EMAIL_NOT_FOUND("SEC_001", "가입되지 않은 이메일입니다", HttpStatus.UNAUTHORIZED),

    INVALID_REFRESH_TOKEN("SEC_002", "유효하지 않은 Refresh Token입니다", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("SEC_003", "로그아웃된 사용자이거나 잘못된 권한입니다", HttpStatus.UNAUTHORIZED),
    MEMBER_NOT_FOUND("SEC_004", "사용자 정보를 찾을 수 없습니다", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    SecurityErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
