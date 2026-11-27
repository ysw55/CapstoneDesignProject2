package com.bookcanvas.domain.user.dto;

/**
 * 회원탈퇴 요청
 *
 * @param password 본인 확인용 비밀번호 (이메일 계정만 필요, 소셜 계정은 비워 둔다)
 */
public record WithdrawRequestDto(String password) {}
