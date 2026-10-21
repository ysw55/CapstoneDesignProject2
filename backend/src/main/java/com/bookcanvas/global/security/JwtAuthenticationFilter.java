package com.bookcanvas.global.security;

import com.bookcanvas.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 요청 헤더의 {@code Authorization: Bearer <token>} 을 읽어 인증 정보를 SecurityContext 에 넣는 필터.
 *
 * <p>토큰이 없거나 잘못돼도 여기서 요청을 막지는 않습니다.
 * 인증이 필요한 API 라면 이후 단계에서 {@link RestAuthenticationEntryPoint} 가 401 을 응답하고,
 * 공개 API(로그인, 회원가입 등)는 그대로 통과합니다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /** 실패 이유를 EntryPoint 에 전달하기 위한 request attribute 키 */
    public static final String AUTH_ERROR_ATTRIBUTE = "bookcanvas.authError";

    private static final List<SimpleGrantedAuthority> USER_AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                AuthUser authUser = jwtTokenProvider.parse(token);
                var authentication = new UsernamePasswordAuthenticationToken(authUser, null, USER_AUTHORITIES);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtTokenProvider.TokenException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.isExpired() ? ErrorCode.EXPIRED_TOKEN : ErrorCode.INVALID_TOKEN);
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
