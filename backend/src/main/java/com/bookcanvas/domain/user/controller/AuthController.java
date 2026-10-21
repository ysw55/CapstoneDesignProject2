package com.bookcanvas.domain.user.controller;

import com.bookcanvas.domain.user.dto.DemoLoginRequestDto;
import com.bookcanvas.domain.user.dto.LoginRequestDto;
import com.bookcanvas.domain.user.dto.PasswordChangeRequestDto;
import com.bookcanvas.domain.user.dto.SignupRequestDto;
import com.bookcanvas.domain.user.dto.TokenResponseDto;
import com.bookcanvas.domain.user.dto.UserResponseDto;
import com.bookcanvas.domain.user.service.AuthService;
import com.bookcanvas.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 인증 API
 *
 * <p><b>Base URL:</b> {@code /api/auth}
 * <ul>
 *   <li>{@code POST /signup} 회원가입 → 201 + 토큰 (공개)</li>
 *   <li>{@code POST /login}  로그인 → 200 + 토큰 (공개)</li>
 *   <li>{@code GET  /me}     내 정보 → 200 (JWT 필요) — 앱 재실행 시 로그인 상태 복원용</li>
 *   <li>{@code POST /password}   비밀번호 변경 → 204 (JWT 필요)</li>
 *   <li>{@code POST /demo-login} 시연용 카카오/구글 간편 로그인 → 200 + 토큰 (공개, 실제 OAuth 아님)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<TokenResponseDto> signup(@Valid @RequestBody SignupRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> me(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(authService.getMe(authUser.userId()));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PasswordChangeRequestDto request
    ) {
        authService.changePassword(authUser.userId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/demo-login")
    public ResponseEntity<TokenResponseDto> demoLogin(@Valid @RequestBody DemoLoginRequestDto request) {
        return ResponseEntity.ok(authService.demoLogin(request.provider()));
    }
}
