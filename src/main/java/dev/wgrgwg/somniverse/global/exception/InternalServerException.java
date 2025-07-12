package dev.wgrgwg.somniverse.global.exception;

import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import lombok.Getter;

@Getter
public class InternalServerException extends RuntimeException {

    private final ErrorCode errorCode;

    public InternalServerException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
