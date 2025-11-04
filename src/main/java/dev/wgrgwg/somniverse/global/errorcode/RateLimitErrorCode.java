package dev.wgrgwg.somniverse.global.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum RateLimitErrorCode implements ErrorCode {

    TOO_MANY_REQUESTS("RATE_LIMIT_001", "요청이 너무 많습니다. 잠시 후 다시 시도하세요.",
        HttpStatus.TOO_MANY_REQUESTS);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    RateLimitErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
