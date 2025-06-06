package dev.wgrgwg.somniverse.global.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT("COMMON_001", "입력 값이 유효하지 않습니다", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("COMMON_002", "서버 오류가 발생하였습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED("COMMON_003", "권한이 없습니다", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CommonErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
