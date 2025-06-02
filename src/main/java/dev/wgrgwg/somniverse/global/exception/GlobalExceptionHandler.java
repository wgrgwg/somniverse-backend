package dev.wgrgwg.somniverse.global.exception;

import dev.wgrgwg.somniverse.global.dto.ApiResponseDto;
import dev.wgrgwg.somniverse.global.dto.FieldErrorDto;
import dev.wgrgwg.somniverse.member.exception.EmailAlreadyExistsException;
import dev.wgrgwg.somniverse.member.exception.UsernameAlreadyExistsException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponseDto.error("입력 값이 유효하지 않습니다.", fieldErrors));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleEmailAlreadyExistsException(
        EmailAlreadyExistsException ex) {
        log.error("이메일 중복 예외 발생", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponseDto.error(ex.getMessage()));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleUsernameAlreadyExistsException(
        UsernameAlreadyExistsException ex) {
        log.error("사용자명 중복 예외 발생", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponseDto.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAllUncaughtException(Exception ex) {
        log.error("처리되지 않은 예외 발생: {}", ex.getMessage(), ex);

        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponseDto.error("서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요."));
    }
}
