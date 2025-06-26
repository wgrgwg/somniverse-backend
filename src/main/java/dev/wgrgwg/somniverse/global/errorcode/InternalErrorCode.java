package dev.wgrgwg.somniverse.global.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InternalErrorCode implements ErrorCode {

    TOKEN_PARSING_FAILED("INTERNAL_001", "토큰 파싱 실패 또는 만료된 토큰입니다");

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    InternalErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
