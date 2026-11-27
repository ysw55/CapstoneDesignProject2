package com.bookcanvas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 회원탈퇴 통합 테스트 (H2) */
@SpringBootTest(properties = "app.book-search.yes24-api-key=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WithdrawIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private ResultActions call(HttpMethod method, String url, Object body, String token) throws Exception {
        var req = request(method, url);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mockMvc.perform(req);
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private Map<String, Object> account(String email, String username) {
        return Map.of("email", email, "password", "reading123", "username", username);
    }

    private String signup(String email, String username) throws Exception {
        return json(call(HttpMethod.POST, "/api/auth/signup", account(email, username), null)
                .andExpect(status().isCreated())).get("accessToken").asText();
    }

    private long write(String token, String content) throws Exception {
        return json(call(HttpMethod.POST, "/api/quotes",
                Map.of("bookTitle", "데미안", "author", "헤르만 헤세", "content", content, "tagName", "위로가 필요할 때"), token)
                .andExpect(status().isCreated())).get("quoteId").asLong();
    }

    @Test
    @DisplayName("탈퇴하면 계정·글·책장이 지워지고 남의 글은 남으며, 같은 이메일로 다시 가입할 수 있다")
    void withdraw() throws Exception {
        String leaving = signup("bye@bookcanvas.com", "떠나는이");
        String staying = signup("stay@bookcanvas.com", "남는이");
        long mine = write(leaving, "떠나는 이의 문장");
        long theirs = write(staying, "남는 이의 문장");

        // 서로 좋아요 · 저장 · 댓글 · 신고 · 차단을 남겨 모든 참조가 있는 상태를 만든다
        for (String[] pair : new String[][]{{leaving, String.valueOf(theirs)}, {staying, String.valueOf(mine)}}) {
            String base = "/api/quotes/" + pair[1];
            call(HttpMethod.POST, base + "/like", null, pair[0]).andExpect(status().isOk());
            call(HttpMethod.POST, base + "/bookmark", null, pair[0]).andExpect(status().isOk());
            call(HttpMethod.POST, base + "/comments", Map.of("content", "댓글"), pair[0]).andExpect(status().isCreated());
            call(HttpMethod.POST, base + "/report", Map.of("reason", "OTHER"), pair[0]).andExpect(status().isNoContent());
        }
        long leavingId = json(call(HttpMethod.GET, "/api/auth/me", null, leaving)).get("userId").asLong();
        long stayingId = json(call(HttpMethod.GET, "/api/auth/me", null, staying)).get("userId").asLong();
        call(HttpMethod.POST, "/api/users/" + stayingId + "/block", null, leaving).andExpect(status().isNoContent());
        call(HttpMethod.POST, "/api/users/" + leavingId + "/block", null, staying).andExpect(status().isNoContent());
        call(HttpMethod.GET, "/api/books/shelves", null, leaving).andExpect(status().isOk());
        call(HttpMethod.POST, "/api/books/completions",
                Map.of("title", "데미안", "author", "헤르만 헤세", "completedAt", "2026-10-01", "coverColor", "#212529"), leaving)
                .andExpect(status().isCreated());

        call(HttpMethod.POST, "/api/auth/withdraw", Map.of("password", "reading123"), null).andExpect(status().isUnauthorized());
        call(HttpMethod.POST, "/api/auth/withdraw", Map.of(), leaving)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
        call(HttpMethod.POST, "/api/auth/withdraw", Map.of("password", "wrong1234"), leaving).andExpect(status().isBadRequest());
        call(HttpMethod.POST, "/api/auth/withdraw", Map.of("password", "reading123"), leaving).andExpect(status().isNoContent());

        call(HttpMethod.GET, "/api/auth/me", null, leaving).andExpect(status().isNotFound());
        call(HttpMethod.POST, "/api/auth/login", Map.of("email", "bye@bookcanvas.com", "password", "reading123"), null)
                .andExpect(status().isUnauthorized());
        JsonNode feed = json(call(HttpMethod.GET, "/api/quotes", null, null)).get("content");
        assertThat(feed.findValuesAsText("content")).containsExactly("남는 이의 문장");
        assertThat(feed.get(0).get("likeCount").asLong()).isZero();
        assertThat(feed.get(0).get("commentCount").asLong()).isZero();
        assertThat(json(call(HttpMethod.GET, "/api/users/me/blocks", null, staying))).isEmpty();
        signup("bye@bookcanvas.com", "떠나는이");
    }

    @Test
    @DisplayName("소셜(시연) 계정은 비밀번호 없이 탈퇴한다")
    void withdrawSocialAccount() throws Exception {
        String token = json(call(HttpMethod.POST, "/api/auth/demo-login", Map.of("provider", "google"), null)
                .andExpect(status().isOk())).get("accessToken").asText();
        call(HttpMethod.POST, "/api/auth/withdraw", Map.of(), token).andExpect(status().isNoContent());
        call(HttpMethod.GET, "/api/auth/me", null, token).andExpect(status().isNotFound());
    }
}
