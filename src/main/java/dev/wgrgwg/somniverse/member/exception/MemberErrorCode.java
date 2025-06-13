package dev.wgrgwg.somniverse.member.exception;

import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MemberErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXISTS("MEMBER_001", "이미 사용중인 이메일입니다", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS("MEMBER_002", "이미 사용중인 사용자명입니다", HttpStatus.CONFLICT),

    EMAIL_NOT_FOUND("MEMBER_003", "가입되지 않은 이메일입니다", HttpStatus.UNAUTHORIZED),

    INVALID_REFRESH_TOKEN("MEMBER_004", "유효하지 않은 Refresh Token입니다", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("MEMBER_005", "로그아웃된 사용자이거나 유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    MEMBER_FOR_TOKEN_NOT_FOUND("MEMBER_006", "토큰에 해당하는 사용자 정보를 찾을 수 없습니다",
        HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    MemberErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
