package dev.wgrgwg.somniverse.global.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum IdempotencyErrorCode implements ErrorCode {
    IDEMPOTENCY_CONFLICT("IDEM_001", "중복 키이지만 페이로드가 다릅니다.",
        HttpStatus.CONFLICT),
    IDEMPOTENCY_IN_PROGRESS("IDEM_002", "요청이 처리 중입니다. 잠시 후 다시 시도하세요.",
        HttpStatus.ACCEPTED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    IdempotencyErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
