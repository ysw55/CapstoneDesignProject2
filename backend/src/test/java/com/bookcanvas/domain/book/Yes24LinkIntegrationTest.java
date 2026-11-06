package com.bookcanvas.domain.book;

import com.bookcanvas.domain.book.dto.BookSearchResponseDto;
import com.bookcanvas.domain.book.dto.BookSearchResultDto;
import com.bookcanvas.domain.book.service.BookSearchService;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 기존 책을 예스24 책 정보로 한꺼번에 연결 (예스24 검색은 가짜로 대체) */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Yes24LinkIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean BookSearchService bookSearchService;

    private ResultActions call(HttpMethod method, String url, Object body, String token) throws Exception {
        var req = request(method, url);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mockMvc.perform(req);
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String signup(String email, String name) throws Exception {
        return json(call(HttpMethod.POST, "/api/auth/signup",
                Map.of("email", email, "password", "reading123", "username", name), null)
                .andExpect(status().isCreated())).get("accessToken").asText();
    }

    private void addBook(String token, String title, String author, String isbn) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("title", title, "author", author, "completedAt", "2026-10-01", "coverColor", "#212529"));
        if (isbn != null) body.put("isbn", isbn);
        call(HttpMethod.POST, "/api/books/completions", body, token).andExpect(status().isCreated());
    }

    private static BookSearchResultDto yes24(String title, String author, String isbn, boolean spine) {
        String id = Integer.toString(Math.abs(title.hashCode()));
        return new BookSearchResultDto(title, author, "출판사", isbn,
                "https://image.yes24.com/goods/" + id + "/L",
                spine ? "https://image.yes24.com/goods/" + id + "/SIDE/L" : null,
                "https://www.yes24.com/product/goods/" + id, "2020-01-01");
    }

    private static BookSearchResponseDto results(BookSearchResultDto... r) {
        return new BookSearchResponseDto(List.of(r), false);
    }

    private JsonNode bookByTitle(String token, String title) throws Exception {
        for (JsonNode b : json(call(HttpMethod.GET, "/api/books/completions", null, token))) {
            if (b.get("book").get("title").asText().equals(title)) return b;
        }
        throw new AssertionError("책을 찾을 수 없음: " + title);
    }

    @Test
    @DisplayName("연결: ISBN 으로 정확히, ISBN 없으면 제목+저자로, 애매하거나 없으면 건드리지 않고 '찾아 봄' 으로 기록")
    void linksExistingBooks() throws Exception {
        String token = signup("link@bookcanvas.com", "연결주인");
        addBook(token, "미움받을 용기", "기시미 이치로", "9788996991342");  // ISBN 있음
        addBook(token, "데미안", "헤르만 헤세", null);                      // 직접 입력, 책등 있는 판본이 있음
        addBook(token, "모모", "미하엘 엔데", null);                        // 예스24 제목이 "모모 MOMO"
        addBook(token, "정체불명의 책", "아무개", null);                    // 예스24 에 없음
        addBook(token, "신화", "다른사람", null);                           // 제목은 같지만 저자가 달라 다른 책

        when(bookSearchService.search(anyString(), anyInt())).thenAnswer(inv -> switch ((String) inv.getArgument(0)) {
            case "9788996991342" -> results(yes24("미움받을 용기", "기시미 이치로 저", "9788996991342", true));
            case "데미안" -> results(
                    yes24("초판본 데미안", "헤르만 헤세 저", "9791164453115", true),            // 제목이 '데미안'으로 시작하지 않아 제외
                    yes24("데미안", "헤르만 헤세 저/전영애 역", "9788937460449", false),        // 같은 책, 책등 없음
                    yes24("데미안", "헤르만 헤세 저/이순학 역", "9788914022141", true));        // 같은 책, 책등 있음 → 이쪽을 우선
            case "모모" -> results(
                    yes24("모모 MOMO", "미하엘 엔데 저/한미희 역", "9788973372256", false),
                    yes24("오늘 밤, 세계에서 이 사랑이 사라진다 해도", "이치조 미사키 저", "9791190000000", true));
            case "신화" -> results(yes24("신화", "토머스 불핀치 저", "9788900000000", true));
            default -> results();
        });

        // 로그인 필요
        call(HttpMethod.POST, "/api/books/completions/link-yes24", null, null).andExpect(status().isUnauthorized());

        // 처음에는 5권 모두 '연결 대기'
        for (JsonNode b : json(call(HttpMethod.GET, "/api/books/completions", null, token))) {
            assertThat(b.get("yes24Pending").asBoolean()).isTrue();
        }

        call(HttpMethod.POST, "/api/books/completions/link-yes24", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(3))
                .andExpect(jsonPath("$.notFound").value(2))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.stopped").value(false));

        // ISBN 으로 연결: 책등·표지·링크
        JsonNode courage = bookByTitle(token, "미움받을 용기");
        assertThat(courage.get("spineImageUrl").asText()).contains("/SIDE/L");
        assertThat(courage.get("productUrl").asText()).startsWith("https://www.yes24.com/product/goods/");
        assertThat(courage.get("book").get("coverImageUrl").asText()).startsWith("https://image.yes24.com/goods/");
        assertThat(courage.get("yes24Pending").asBoolean()).isFalse();

        // 제목으로 연결: 같은 책의 여러 판본 중 책등이 있는 판본을 고른다
        JsonNode demian = bookByTitle(token, "데미안");
        assertThat(demian.get("spineImageUrl").isNull()).isFalse();
        assertThat(demian.get("productUrl").asText()).isEqualTo(yes24("데미안", "", "", true).productUrl());

        // "모모" → "모모 MOMO" 로 연결, 책등은 없는 판본 (표지·링크만)
        JsonNode momo = bookByTitle(token, "모모");
        assertThat(momo.get("productUrl").isNull()).isFalse();
        assertThat(momo.get("spineImageUrl").isNull()).isTrue();

        // 못 찾은 책·저자가 다른 책은 건드리지 않고 '찾아 봄' 으로만 기록 → 대기 목록에서 빠진다
        for (String title : List.of("정체불명의 책", "신화")) {
            JsonNode b = bookByTitle(token, title);
            assertThat(b.get("productUrl").isNull()).as(title).isTrue();
            assertThat(b.get("spineImageUrl").isNull()).as(title).isTrue();
            assertThat(b.get("yes24Pending").asBoolean()).as(title).isFalse();
        }

        // 한 번 더 눌러도 다시 찾지 않는다
        call(HttpMethod.POST, "/api/books/completions/link-yes24", null, token)
                .andExpect(jsonPath("$.linked").value(0))
                .andExpect(jsonPath("$.notFound").value(0))
                .andExpect(jsonPath("$.remaining").value(0));
    }

    @Test
    @DisplayName("연결: 예스24 호출이 막히면 중간에 멈추고 남은 책은 그대로 두며, 다른 책이 이미 쓰는 ISBN 은 넣지 않는다")
    void stopsWhenYes24Fails() throws Exception {
        String token = signup("link2@bookcanvas.com", "연결중단");
        addBook(token, "클린 코드", "로버트 마틴", "9788966260959");  // 이 ISBN 은 이미 이 책이 쓴다
        addBook(token, "클린코드", "로버트 마틴", null);              // 같은 책을 직접 입력한 중복 → ISBN 을 또 넣으면 안 된다

        when(bookSearchService.search(anyString(), anyInt())).thenAnswer(inv -> {
            String q = inv.getArgument(0);
            if (q.equals("9788966260959")) return results(yes24("클린 코드", "로버트 마틴 저", "9788966260959", true));
            if (q.equals("클린코드")) return results(yes24("클린 코드", "로버트 마틴 저", "9788966260959", true));
            throw new BusinessException(ErrorCode.BOOK_SEARCH_UNAVAILABLE); // 호출 한도 등
        });

        // 두 권 모두 연결된다 (두 번째는 ISBN 충돌을 피해 ISBN 없이) — 500 오류가 나면 안 된다
        call(HttpMethod.POST, "/api/books/completions/link-yes24", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(2))
                .andExpect(jsonPath("$.stopped").value(false));

        // 이제 새 책을 추가하고 예스24 가 막힌 상황
        addBook(token, "새로 추가한 책", "누군가", null);
        call(HttpMethod.POST, "/api/books/completions/link-yes24", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(0))
                .andExpect(jsonPath("$.remaining").value(1))
                .andExpect(jsonPath("$.stopped").value(true));
        assertThat(bookByTitle(token, "새로 추가한 책").get("yes24Pending").asBoolean()).isTrue(); // 다음에 다시 시도할 수 있다
    }
}
