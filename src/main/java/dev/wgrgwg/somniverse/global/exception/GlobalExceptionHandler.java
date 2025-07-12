package dev.wgrgwg.somniverse.global.exception;

import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import dev.wgrgwg.somniverse.global.dto.FieldErrorDto;
import dev.wgrgwg.somniverse.global.errorcode.CommonErrorCode;
import dev.wgrgwg.somniverse.global.errorcode.ErrorCode;
import dev.wgrgwg.somniverse.member.exception.MemberErrorCode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<List<FieldErrorDto>>> handleValidationExceptions(
        MethodArgumentNotValidException ex) {

        List<FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldErrorDto(error.getField(), error.getDefaultMessage())).toList();
        log.error("유효성 검증 실패: {}", fieldErrors);

        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT.getHttpStatus()).body(
            ApiResponseDto.error(CommonErrorCode.INVALID_INPUT.getMessage(), fieldErrors,
                CommonErrorCode.INVALID_INPUT.getCode()));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.error("예외 발생: {}", ex.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ApiResponseDto.error(errorCode.getMessage(), errorCode.getCode()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleBadCredentialsException(Exception ex) {
        log.error("예외 발생: {}", ex.getMessage());

        return ResponseEntity.status(MemberErrorCode.INVALID_CREDENTIALS.getHttpStatus()).body(
            ApiResponseDto.error(MemberErrorCode.INVALID_CREDENTIALS.getMessage(),
                MemberErrorCode.INVALID_CREDENTIALS.getCode()));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleMissingRequestCookieException(
        Exception ex) {
        log.error("예외 발생: {}", ex.getMessage());

        return ResponseEntity.status(CommonErrorCode.MISSING_COOKIE.getHttpStatus()).body(
            ApiResponseDto.error(CommonErrorCode.MISSING_COOKIE.getMessage(),
                CommonErrorCode.MISSING_COOKIE.getCode()));
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleInternalException(
        InternalServerException ex) {
        log.error("서버 내부 오류: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponseDto.error("서버 내부 오류가 발생하였습니다", ex.getErrorCode().getCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAllUncaughtException(Exception ex) {
        log.error("처리되지 않은 예외 발생: {}", ex.getMessage(), ex);

        return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).body(
            ApiResponseDto.error(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                CommonErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }
}
