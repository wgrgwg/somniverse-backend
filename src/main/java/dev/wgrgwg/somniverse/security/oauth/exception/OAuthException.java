package dev.wgrgwg.somniverse.security.oauth.exception;

import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import lombok.Getter;

@Getter
public class OAuthException extends RuntimeException {

    private final ErrorCode errorCode;

    public OAuthException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
