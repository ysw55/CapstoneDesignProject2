package com.bookcanvas.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 컨트롤러에서 발생한 예외를 {@link ErrorResponse} 형식으로 통일해서 내려줍니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 서비스 계층의 비즈니스 규칙 위반 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.info("[BusinessException] {} - {}", code, e.getMessage());
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, e.getMessage()));
    }

    /** {@code @Valid} DTO 검증 실패 (예: 독후감 200자 미만, 이메일 형식 오류) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        // 첫 번째 필드 에러 메시지를 대표 메시지로 사용 → 프론트에서 바로 띄우기 편함
        String message = fieldErrors.isEmpty() ? ErrorCode.INVALID_INPUT.getMessage() : fieldErrors.get(0).message();
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message, fieldErrors));
    }

    /** JSON 형식이 깨졌거나 날짜 형식이 맞지 않는 경우 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "요청 형식이 올바르지 않아요."));
    }

    /** 예상하지 못한 오류 — 내부 정보는 로그에만 남기고 클라이언트에는 일반 메시지 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // 404(없는 경로), 405(지원하지 않는 메서드) 등 Spring 이 상태 코드를 정해 둔 예외는 그 상태를 유지
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            int status = springError.getStatusCode().value();
            return ResponseEntity.status(status).body(new ErrorResponse(
                    status, "HTTP_" + status, springError.getBody().getTitle(), List.of(), java.time.LocalDateTime.now()));
        }
        log.error("[UnexpectedException]", e);
        return ResponseEntity.internalServerError().body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}
