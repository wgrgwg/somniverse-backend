package dev.wgrgwg.somniverse.dream.exception;

import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DreamErrorCode implements ErrorCode {
    DREAM_NOT_FOUND("DREAM_001", "해당 꿈일기를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    DREAM_FORBIDDEN("DREAM_002", "해당 꿈일기에 대한 권한이 없습니다", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    DreamErrorCode(final String code, final String message, final HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
