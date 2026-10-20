package com.bookcanvas.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모든 API 에러의 공통 응답 형식
 *
 * <pre>{@code
 * {
 *   "status": 400,
 *   "code": "REVIEW_TOO_SHORT",
 *   "message": "독후감은 최소 200자 이상 작성해야 완독 인증이 가능해요. (현재 150자)",
 *   "fieldErrors": [ { "field": "reviewText", "message": "..." } ],   // 입력값 검증 실패 시에만
 *   "timestamp": "2026-10-20T14:00:00"
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldError> fieldErrors,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.name(), message, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.name(), message, fieldErrors, LocalDateTime.now());
    }

    public record FieldError(String field, String message) {}
}
