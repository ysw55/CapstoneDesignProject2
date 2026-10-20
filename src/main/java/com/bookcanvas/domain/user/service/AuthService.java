package com.bookcanvas.domain.user.service;

import com.bookcanvas.domain.book.repository.BookCompletionRepository;
import com.bookcanvas.domain.user.dto.LoginRequestDto;
import com.bookcanvas.domain.user.dto.PasswordChangeRequestDto;
import com.bookcanvas.domain.user.dto.SignupRequestDto;
import com.bookcanvas.domain.user.dto.TokenResponseDto;
import com.bookcanvas.domain.user.dto.UserResponseDto;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.config.DemoProperties;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import com.bookcanvas.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * 회원가입 / 로그인 / 내 정보 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final BookCompletionRepository bookCompletionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final DemoProperties demoProperties;

    /**
     * 회원가입 후 바로 로그인된 상태가 되도록 토큰까지 발급합니다.
     *
     * @throws BusinessException DUPLICATE_EMAIL, DUPLICATE_USERNAME
     */
    @Transactional
    public TokenResponseDto signup(SignupRequestDto request) {
        String email = normalizeEmail(request.email());
        String username = request.username().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        User user = User.builder()
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(request.password())) // 평문 비밀번호는 저장하지 않음
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 위 중복 검사와 저장 사이에 같은 이메일로 동시에 가입한 경우 (DB unique 제약이 최종 방어선)
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        log.info("[Auth] 회원가입 완료 - userId={}", user.getId());
        return issueToken(user);
    }

    /**
     * 이메일·비밀번호를 확인하고 Access Token 을 발급합니다.
     * 이메일이 없는 경우와 비밀번호가 틀린 경우를 같은 메시지로 응답해 가입 여부가 드러나지 않게 합니다.
     *
     * @throws BusinessException LOGIN_FAILED
     */
    public TokenResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        log.info("[Auth] 로그인 - userId={}", user.getId());
        return issueToken(user);
    }

    /** 토큰의 userId 로 최신 사용자 정보(포인트, 스킨 등) 조회 */
    public UserResponseDto getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toResponse(user);
    }

    /**
     * 비밀번호 변경 — 현재 비밀번호로 본인 확인 후 새 비밀번호를 BCrypt 로 저장
     *
     * <p>참고: JWT 는 서버에 상태가 없으므로 이미 발급된 토큰은 만료 시각까지 유효합니다.
     * (다른 기기 강제 로그아웃이 필요하면 토큰 버전 컬럼을 두고 필터에서 비교하는 방식으로 확장)
     *
     * @throws BusinessException SOCIAL_ACCOUNT_PASSWORD, INVALID_CURRENT_PASSWORD, SAME_AS_CURRENT_PASSWORD
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isSocialAccount()) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_PASSWORD);
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        log.info("[Auth] 비밀번호 변경 - userId={}", userId);
    }

    /**
     * 시연용 간편 로그인 (카카오 / 구글)
     *
     * <p>⚠ 실제 OAuth 인증이 아닙니다. 버튼을 누르면 provider 별 고정 시연 계정으로 로그인되며,
     * {@code app.demo.login-enabled=false} 로 끌 수 있습니다. 실서비스에서는 반드시 꺼 두세요.
     */
    @Transactional
    public TokenResponseDto demoLogin(String provider) {
        if (!demoProperties.loginEnabled()) {
            throw new BusinessException(ErrorCode.DEMO_LOGIN_DISABLED);
        }
        DemoAccounts account = DemoAccounts.of(provider);

        User user = userRepository.findByEmail(account.email())
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(account.email())
                        .username(account.username())
                        // 소셜 계정은 비밀번호 로그인을 쓰지 않으므로 추측 불가능한 값으로 채움
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .provider(account.provider())
                        .build()));

        if (user.getProvider() != account.provider()) {
            // 누군가 같은 이메일로 일반 가입해 둔 경우 — 그 계정으로 로그인시키지 않음
            throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER);
        }

        log.info("[Auth] 시연용 간편 로그인 - provider={}, userId={}", account.provider(), user.getId());
        return issueToken(user);
    }

    // =========================================================================

    private TokenResponseDto issueToken(User user) {
        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        return TokenResponseDto.bearer(token, jwtTokenProvider.getValiditySeconds(), toResponse(user));
    }

    private UserResponseDto toResponse(User user) {
        return UserResponseDto.of(user, bookCompletionRepository.countByUserIdAndCertifiedAtIsNotNull(user.getId()));
    }

    /** "Reader@Mail.com " 과 "reader@mail.com" 을 같은 계정으로 취급 */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
