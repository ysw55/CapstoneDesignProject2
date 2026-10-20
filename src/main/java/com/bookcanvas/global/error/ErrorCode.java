package com.bookcanvas.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 에러 코드 목록.
 * 프론트엔드는 {@code code} 값으로 분기하고, {@code message} 는 사용자에게 그대로 보여줄 수 있습니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ──
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 다시 확인해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했어요. 잠시 후 다시 시도해 주세요."),

    // ── 인증 ──
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증 정보가 올바르지 않아요. 다시 로그인해 주세요."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 만료됐어요. 다시 로그인해 주세요."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않아요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없어요."),
    DEMO_LOGIN_DISABLED(HttpStatus.FORBIDDEN, "시연용 간편 로그인이 꺼져 있어요."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식이에요."),

    // ── 회원 ──
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일이에요."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임이에요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자예요."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않아요."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호가 현재 비밀번호와 같아요."),
    SOCIAL_ACCOUNT_PASSWORD(HttpStatus.BAD_REQUEST, "소셜 로그인 계정은 비밀번호를 변경할 수 없어요."),

    // ── 내 책장 / 완독 인증 ──
    REVIEW_TOO_SHORT(HttpStatus.BAD_REQUEST, "독후감은 최소 200자 이상 작성해야 완독 인증이 가능해요."),
    INVALID_COMPLETION_DATE(HttpStatus.BAD_REQUEST, "완독 날짜는 오늘 이후로 설정할 수 없어요."),
    ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완독 인증한 책이에요."),
    ALREADY_ON_SHELF(HttpStatus.CONFLICT, "이미 책장에 있는 책이에요."),
    SHELF_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "책장에서 책을 찾을 수 없어요."),
    CERTIFIED_BOOK_LOCKED(HttpStatus.CONFLICT, "완독 인증한 책은 표지 색만 바꿀 수 있고, 삭제할 수 없어요."),

    // ── 커뮤니티 ──
    QUOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "글을 찾을 수 없어요. 삭제됐을 수 있어요."),
    QUOTE_FORBIDDEN(HttpStatus.FORBIDDEN, "내가 쓴 글만 수정·삭제할 수 있어요."),
    INVALID_TAG(HttpStatus.BAD_REQUEST, "존재하지 않는 상황 태그예요.");

    private final HttpStatus status;
    private final String message;
}
