package com.bookcanvas;

import com.bookcanvas.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원가입 → 로그인 → JWT 로 완독 인증까지 실제 HTTP 흐름을 검증하는 통합 테스트.
 * Security 필터, 입력값 검증, 예외 응답 형식, DB 저장까지 모두 실제 빈으로 동작합니다. (DB 는 H2)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthAndCompletionIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;

    private static final String REVIEW_200 = "나".repeat(200);

    private ResultActions postJson(String url, Object body, String token) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mockMvc.perform(request);
    }

    private String signupAndGetToken(String email, String username) throws Exception {
        String json = postJson("/api/auth/signup", Map.of("email", email, "password", "reading123", "username", username), null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("accessToken").asText();
    }

    private Map<String, Object> completion(String title, String review) {
        return Map.of("title", title, "author", "헤르만 헤세", "completedAt", "2026-10-01", "reviewText", review);
    }

    // ── 회원 인증 ─────────────────────────────────────────

    @Test
    @DisplayName("회원가입: 비밀번호는 BCrypt 로 암호화되어 저장되고, 바로 토큰이 발급된다")
    void signup_encodesPassword_andIssuesToken() throws Exception {
        postJson("/api/auth/signup", Map.of("email", "Reader@BookCanvas.com ", "password", "reading123", "username", "책벌레"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("reader@bookcanvas.com")) // 소문자·공백 정리
                .andExpect(jsonPath("$.user.rewardPoint").value(0))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        String stored = userRepository.findByEmail("reader@bookcanvas.com").orElseThrow().getPassword();
        assertThat(stored).isNotEqualTo("reading123").startsWith("$2"); // BCrypt 해시
    }

    @Test
    @DisplayName("회원가입: 이메일 중복은 409, 비밀번호 규칙 위반은 400 + 필드 에러")
    void signup_validation() throws Exception {
        signupAndGetToken("dup@bookcanvas.com", "중복테스트");

        postJson("/api/auth/signup", Map.of("email", "dup@bookcanvas.com", "password", "reading123", "username", "다른닉네임"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));

        postJson("/api/auth/signup", Map.of("email", "new@bookcanvas.com", "password", "onlyletters", "username", "새유저"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    @DisplayName("로그인: 성공하면 토큰 발급, 비밀번호가 틀리거나 없는 이메일이면 같은 401 메시지")
    void login() throws Exception {
        signupAndGetToken("login@bookcanvas.com", "로그인테스트");

        postJson("/api/auth/login", Map.of("email", "login@bookcanvas.com", "password", "reading123"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("로그인테스트"));

        String wrongPw = postJson("/api/auth/login", Map.of("email", "login@bookcanvas.com", "password", "wrong1234"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
                .andReturn().getResponse().getContentAsString();
        String noUser = postJson("/api/auth/login", Map.of("email", "nobody@bookcanvas.com", "password", "reading123"), null)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(wrongPw).get("message")).isEqualTo(objectMapper.readTree(noUser).get("message"));
    }

    @Test
    @DisplayName("/api/auth/me: 토큰이 없으면 401 UNAUTHORIZED, 위조 토큰이면 401 INVALID_TOKEN, 정상 토큰이면 내 정보")
    void me_requiresValidToken() throws Exception {
        String token = signupAndGetToken("me@bookcanvas.com", "내정보");

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token + "tampered"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@bookcanvas.com"));
    }

    // ── 완독 인증 ─────────────────────────────────────────

    @Test
    @DisplayName("완독 인증: JWT 없이 요청하면 401")
    void completion_requiresAuth() throws Exception {
        postJson("/api/books/completion", completion("데미안", REVIEW_200), null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("완독 인증: 독후감 199자는 400, 공백으로만 200자를 채워도 400(REVIEW_TOO_SHORT)")
    void completion_rejectsShortReview() throws Exception {
        String token = signupAndGetToken("short@bookcanvas.com", "짧은글");

        postJson("/api/books/completion", completion("데미안", "나".repeat(199)), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reviewText"));

        postJson("/api/books/completion", completion("데미안", "  " + "나".repeat(199)), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REVIEW_TOO_SHORT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("현재 199자")));
    }

    @Test
    @DisplayName("완독 인증: 성공 시 201 + 보상 지급, 내 정보에 포인트·완독 수 반영, 같은 책 재인증은 409")
    void completion_success_thenDuplicate() throws Exception {
        String token = signupAndGetToken("reader@bookcanvas.com", "완독러");

        String json = postJson("/api/books/completion", completion("데미안", REVIEW_200), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.book.title").value("데미안"))
                .andExpect(jsonPath("$.completedAt").value("2026-10-01"))
                .andExpect(jsonPath("$.reviewLength").value(200))
                .andExpect(jsonPath("$.reward.earnedPoint").value(100))
                .andExpect(jsonPath("$.reward.totalPoint").value(100))
                .andExpect(jsonPath("$.reward.profileBackgroundColor").value("#1B4D3E"))
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(json);
        assertThat(body.get("completionId").asLong()).isPositive();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.rewardPoint").value(100))
                .andExpect(jsonPath("$.completedBookCount").value(1))
                .andExpect(jsonPath("$.profileBackgroundColor").value("#1B4D3E"));

        postJson("/api/books/completion", completion("데미안", REVIEW_200), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_COMPLETED"));

        // 다른 책은 인증 가능 → 포인트 누적
        postJson("/api/books/completion", completion("수레바퀴 아래서", REVIEW_200), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reward.totalPoint").value(200));
    }

    @Test
    @DisplayName("완독 인증: 같은 책이라도 다른 사용자는 각자 인증할 수 있다 (Book 은 재사용)")
    void completion_sameBook_differentUsers() throws Exception {
        String tokenA = signupAndGetToken("a@bookcanvas.com", "독자A");
        String tokenB = signupAndGetToken("b@bookcanvas.com", "독자B");

        long bookA = objectMapper.readTree(postJson("/api/books/completion", completion("데미안", REVIEW_200), tokenA)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).at("/book/bookId").asLong();
        long bookB = objectMapper.readTree(postJson("/api/books/completion", completion("데미안", REVIEW_200), tokenB)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).at("/book/bookId").asLong();

        assertThat(bookA).isEqualTo(bookB);
    }

    @Test
    @DisplayName("완독 인증: 미래 날짜는 400")
    void completion_rejectsFutureDate() throws Exception {
        String token = signupAndGetToken("future@bookcanvas.com", "미래인");

        postJson("/api/books/completion",
                Map.of("title", "데미안", "author", "헤르만 헤세", "completedAt", "2999-01-01", "reviewText", REVIEW_200), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("completedAt"));
    }
}
