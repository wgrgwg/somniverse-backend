package dev.wgrgwg.somniverse.member.exception;

import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MemberErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXISTS("AUTH_001", "이미 사용중인 이메일입니다", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS("AUTH_002", "이미 사용중인 사용자명입니다", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    MemberErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
