package com.bookcanvas.global.error;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 던지는 예외.
 * {@link GlobalExceptionHandler} 가 {@link ErrorCode} 의 HTTP 상태와 메시지로 응답을 만듭니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 기본 메시지 대신 상황에 맞는 메시지를 쓰고 싶을 때 (예: 현재 글자 수 포함) */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
