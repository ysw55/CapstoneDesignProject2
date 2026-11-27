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

/** 커뮤니티 글 신고 · 사용자 차단 통합 테스트 (H2) */
@SpringBootTest(properties = "app.book-search.yes24-api-key=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModerationIntegrationTest {

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

    private String signup(String email, String username) throws Exception {
        return json(call(HttpMethod.POST, "/api/auth/signup",
                Map.of("email", email, "password", "reading123", "username", username), null)
                .andExpect(status().isCreated())).get("accessToken").asText();
    }

    private JsonNode write(String token, String content) throws Exception {
        return json(call(HttpMethod.POST, "/api/quotes",
                Map.of("bookTitle", "데미안", "author", "헤르만 헤세", "content", content, "tagName", "위로가 필요할 때"), token)
                .andExpect(status().isCreated()));
    }

    private JsonNode feed(String token) throws Exception {
        return json(call(HttpMethod.GET, "/api/quotes?size=50", null, token).andExpect(status().isOk())).get("content");
    }

    @Test
    @DisplayName("신고한 글은 신고한 사람에게만 안 보이고, 내 글은 신고할 수 없고, 신고된 글도 작성자가 지울 수 있다")
    void report() throws Exception {
        String writer = signup("writer@bookcanvas.com", "글쓴이");
        String reader = signup("reader@bookcanvas.com", "읽는이");
        long quoteId = write(writer, "신고 대상 문장").get("quoteId").asLong();
        String url = "/api/quotes/" + quoteId + "/report";

        call(HttpMethod.POST, url, Map.of("reason", "SPAM"), null).andExpect(status().isUnauthorized());
        call(HttpMethod.POST, url, Map.of(), reader).andExpect(status().isBadRequest());
        call(HttpMethod.POST, url, Map.of("reason", "SPAM"), writer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_REPORT_OWN_QUOTE"));
        call(HttpMethod.POST, url, Map.of("reason", "SPAM"), reader).andExpect(status().isNoContent());
        call(HttpMethod.POST, url, Map.of("reason", "ABUSE"), reader).andExpect(status().isNoContent());

        assertThat(feed(reader).findValuesAsText("content")).doesNotContain("신고 대상 문장");
        assertThat(feed(writer).findValuesAsText("content")).contains("신고 대상 문장");
        assertThat(feed(null).findValuesAsText("content")).contains("신고 대상 문장");

        call(HttpMethod.DELETE, "/api/quotes/" + quoteId, null, writer).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("차단하면 상대의 글·댓글·저장 목록이 숨고, 해제하면 다시 보인다")
    void block() throws Exception {
        String me = signup("me@bookcanvas.com", "차단하는이");
        String other = signup("other@bookcanvas.com", "차단대상");
        JsonNode theirs = write(other, "차단 대상의 문장");
        long theirQuote = theirs.get("quoteId").asLong();
        long otherId = theirs.get("author").get("userId").asLong();
        JsonNode mine = write(me, "내 문장");
        long myQuote = mine.get("quoteId").asLong();
        long myId = mine.get("author").get("userId").asLong();
        call(HttpMethod.POST, "/api/quotes/" + myQuote + "/comments", Map.of("content", "상대 댓글"), other)
                .andExpect(status().isCreated());
        call(HttpMethod.POST, "/api/quotes/" + theirQuote + "/bookmark", null, me).andExpect(status().isOk());

        call(HttpMethod.POST, "/api/users/" + myId + "/block", null, me)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_BLOCK_SELF"));
        call(HttpMethod.GET, "/api/users/me/blocks", null, null).andExpect(status().isUnauthorized());
        call(HttpMethod.POST, "/api/users/" + otherId + "/block", null, me).andExpect(status().isNoContent());
        call(HttpMethod.POST, "/api/users/" + otherId + "/block", null, me).andExpect(status().isNoContent());

        assertThat(feed(me).findValuesAsText("content")).contains("내 문장").doesNotContain("차단 대상의 문장");
        assertThat(feed(other).findValuesAsText("content")).contains("내 문장", "차단 대상의 문장");
        assertThat(json(call(HttpMethod.GET, "/api/quotes/" + myQuote + "/comments", null, me))).isEmpty();
        assertThat(json(call(HttpMethod.GET, "/api/quotes/" + myQuote + "/comments", null, null))).hasSize(1);
        feed(me).forEach(q -> {
            if (q.get("quoteId").asLong() == myQuote) assertThat(q.get("commentCount").asLong()).isZero();
        });
        assertThat(json(call(HttpMethod.GET, "/api/quotes/me/bookmarked", null, me)).get("totalElements").asLong()).isZero();
        assertThat(json(call(HttpMethod.GET, "/api/users/me/blocks", null, me)).findValuesAsText("username"))
                .containsExactly("차단대상");

        call(HttpMethod.DELETE, "/api/users/" + otherId + "/block", null, me).andExpect(status().isNoContent());
        assertThat(feed(me).findValuesAsText("content")).contains("차단 대상의 문장");
        assertThat(json(call(HttpMethod.GET, "/api/quotes/me/bookmarked", null, me)).get("totalElements").asLong()).isEqualTo(1);
    }
}
