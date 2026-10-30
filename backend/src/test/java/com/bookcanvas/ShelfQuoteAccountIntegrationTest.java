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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 내 책장 동기화 · 커뮤니티 피드 · 비밀번호 변경 · 시연용 간편 로그인 통합 테스트 (H2)
 */
// 개발자 PC 의 application-local.yml 에 카카오 키가 있어도 '키 없음' 상태로 테스트한다
@SpringBootTest(properties = "app.book-search.kakao-rest-api-key=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShelfQuoteAccountIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String REVIEW = "다".repeat(210);

    private ResultActions call(HttpMethod method, String url, Object body, String token) throws Exception {
        var req = request(method, url);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mockMvc.perform(req);
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String signup(String email, String username) throws Exception {
        return json(call(HttpMethod.POST, "/api/auth/signup",
                Map.of("email", email, "password", "reading123", "username", username), null)
                .andExpect(status().isCreated())).get("accessToken").asText();
    }

    private Map<String, Object> shelfBook(String title, String date) {
        return Map.of("title", title, "author", "헤르만 헤세", "completedAt", date, "coverColor", "#212529");
    }

    // ── 내 책장 ─────────────────────────────────────────

    @Test
    @DisplayName("책장 순서: 저장하면 그 순서로 조회, 새 책은 맨 앞, 잘못된 목록은 400")
    void shelfReorder() throws Exception {
        String token = signup("order@bookcanvas.com", "순서주인");
        long a = json(call(HttpMethod.POST, "/api/books/completions", shelfBook("책A", "2026-10-01"), token)).get("completionId").asLong();
        long b = json(call(HttpMethod.POST, "/api/books/completions", shelfBook("책B", "2026-10-02"), token)).get("completionId").asLong();
        long c = json(call(HttpMethod.POST, "/api/books/completions", shelfBook("책C", "2026-10-03"), token)).get("completionId").asLong();

        // 정한 적 없으면 최근 완독 순: C, B, A
        JsonNode shelf = json(call(HttpMethod.GET, "/api/books/completions", null, token));
        assertThat(shelf.findValuesAsText("title")).containsExactly("책C", "책B", "책A");

        // A, C, B 순으로 저장
        call(HttpMethod.PUT, "/api/books/completions/order", Map.of("completionIds", java.util.List.of(a, c, b)), token)
                .andExpect(status().isNoContent());
        shelf = json(call(HttpMethod.GET, "/api/books/completions", null, token));
        assertThat(shelf.findValuesAsText("title")).containsExactly("책A", "책C", "책B");
        assertThat(shelf.get(0).get("shelfOrder").asInt()).isEqualTo(0);

        // 새로 꽂은 책은 정한 순서와 상관없이 맨 앞
        call(HttpMethod.POST, "/api/books/completions", shelfBook("책D", "2026-09-01"), token).andExpect(status().isCreated());
        shelf = json(call(HttpMethod.GET, "/api/books/completions", null, token));
        assertThat(shelf.findValuesAsText("title")).containsExactly("책D", "책A", "책C", "책B");

        // 일부만 보내거나 중복·없는 id → 400
        call(HttpMethod.PUT, "/api/books/completions/order", Map.of("completionIds", java.util.List.of(a, b)), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SHELF_ORDER"));
        call(HttpMethod.PUT, "/api/books/completions/order", Map.of("completionIds", java.util.List.of(a, a, b, c)), token)
                .andExpect(status().isBadRequest());

        // 남의 책장 id 가 섞이면 400
        String other = signup("order2@bookcanvas.com", "다른사람");
        long x = json(call(HttpMethod.POST, "/api/books/completions", shelfBook("책X", "2026-10-01"), other)).get("completionId").asLong();
        call(HttpMethod.PUT, "/api/books/completions/order", Map.of("completionIds", java.util.List.of(a, b, c, x)), token)
                .andExpect(status().isBadRequest());

        // 로그인 없이는 401
        call(HttpMethod.PUT, "/api/books/completions/order", Map.of("completionIds", java.util.List.of()), null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("보관함: 인증한 책도 보관·꺼내기 가능, 보관해도 인증·포인트 유지, 순서 저장은 책장 책만 대상")
    void shelfArchive() throws Exception {
        String token = signup("archive@bookcanvas.com", "보관주인");
        long a = json(call(HttpMethod.POST, "/api/books/completions", shelfBook("책A", "2026-10-01"), token)).get("completionId").asLong();
        long b = json(call(HttpMethod.POST, "/api/books/completions", shelfBook("책B", "2026-10-02"), token)).get("completionId").asLong();

        // 책A 완독 인증 → 삭제는 불가(409)
        call(HttpMethod.POST, "/api/books/completion",
                Map.of("title", "책A", "author", "헤르만 헤세", "completedAt", "2026-10-01", "reviewText", REVIEW), token)
                .andExpect(status().isCreated());
        call(HttpMethod.DELETE, "/api/books/completions/" + a, null, token).andExpect(status().isConflict());

        // 인증한 책도 보관함으로 옮길 수 있고, 인증·포인트는 그대로
        call(HttpMethod.POST, "/api/books/completions/" + a + "/archive", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true))
                .andExpect(jsonPath("$.certified").value(true))
                .andExpect(jsonPath("$.earnedPoint").value(100));

        // 같은 책을 다시 추가하려 하면 '보관함에 있음' 안내
        call(HttpMethod.POST, "/api/books/completions", shelfBook("책A", "2026-10-03"), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_ARCHIVED"));

        // 순서 저장은 책장에 있는 책(B)만 보낸다. 보관한 책(A)을 섞으면 400
        call(HttpMethod.PUT, "/api/books/completions/order", Map.of("completionIds", java.util.List.of(b)), token)
                .andExpect(status().isNoContent());
        call(HttpMethod.PUT, "/api/books/completions/order", Map.of("completionIds", java.util.List.of(a, b)), token)
                .andExpect(status().isBadRequest());

        // 꺼내면 책장 맨 앞으로 돌아온다
        call(HttpMethod.DELETE, "/api/books/completions/" + a + "/archive", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.shelfOrder").doesNotExist());
        JsonNode shelf = json(call(HttpMethod.GET, "/api/books/completions", null, token));
        assertThat(shelf.findValuesAsText("title")).containsExactly("책A", "책B");

        // 남의 책은 보관할 수 없다
        String other = signup("archive2@bookcanvas.com", "다른사람");
        call(HttpMethod.POST, "/api/books/completions/" + a + "/archive", null, other).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("여러 책장: 기본 책장 자동 생성, 만들기·이름 변경·최대 5개, 책 옮기기, 책장별 순서, 삭제 시 책 이동")
    void multipleShelves() throws Exception {
        String token = signup("shelves@bookcanvas.com", "책장여럿");

        // 책장 기능 이전처럼 책장 지정 없이 책을 꽂아도 첫 번째 책장에 들어간다
        long a = json(call(HttpMethod.POST, "/api/books/completions", shelfBook("책A", "2026-10-01"), token)).get("completionId").asLong();
        JsonNode shelves = json(call(HttpMethod.GET, "/api/books/shelves", null, token).andExpect(status().isOk()));
        assertThat(shelves).hasSize(1);
        assertThat(shelves.get(0).get("name").asText()).isEqualTo("책장 1");
        assertThat(shelves.get(0).get("bookCount").asInt()).isEqualTo(1);
        long first = shelves.get(0).get("shelfId").asLong();

        // 책장 만들기 — 이름을 비우면 "책장 2", 지으면 그 이름
        long second = json(call(HttpMethod.POST, "/api/books/shelves", Map.of(), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("책장 2"))
                .andExpect(jsonPath("$.bookCount").value(0))).get("shelfId").asLong();
        call(HttpMethod.PUT, "/api/books/shelves/" + second, Map.of("name", "소설"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("소설"));
        call(HttpMethod.PUT, "/api/books/shelves/" + second, Map.of("name", "  "), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKSHELF_NAME_REQUIRED"));

        // 특정 책장에 바로 꽂기
        Map<String, Object> bBody = new java.util.HashMap<>(shelfBook("책B", "2026-10-02"));
        bBody.put("shelfId", second);
        long b = json(call(HttpMethod.POST, "/api/books/completions", bBody, token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shelfId").value(second))).get("completionId").asLong();

        // 최대 5개 (현재 2개 → 3개 더 만들면 5개, 그다음은 409)
        for (int i = 0; i < 3; i++) call(HttpMethod.POST, "/api/books/shelves", Map.of(), token).andExpect(status().isCreated());
        call(HttpMethod.POST, "/api/books/shelves", Map.of(), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKSHELF_LIMIT_REACHED"));

        // 책 옮기기: A 를 소설 책장으로 → 소설 책장 맨 앞
        call(HttpMethod.PUT, "/api/books/completions/" + a + "/shelf", Map.of("shelfId", second), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shelfId").value(second));

        // 책장별 순서 저장: 소설 책장의 B, A 순서로 — 그 책장의 책이 아닌 id 가 섞이면 400
        call(HttpMethod.PUT, "/api/books/completions/order", Map.of("shelfId", second, "completionIds", java.util.List.of(b, a)), token)
                .andExpect(status().isNoContent());
        call(HttpMethod.PUT, "/api/books/completions/order", Map.of("shelfId", first, "completionIds", java.util.List.of(a)), token)
                .andExpect(status().isBadRequest());

        // 소설 책장 삭제 → 책 2권은 첫 번째 책장으로
        call(HttpMethod.DELETE, "/api/books/shelves/" + second, null, token).andExpect(status().isNoContent());
        JsonNode shelf = json(call(HttpMethod.GET, "/api/books/completions", null, token));
        assertThat(shelf).hasSize(2);
        for (JsonNode book : shelf) assertThat(book.get("shelfId").asLong()).isEqualTo(first);

        // 보관함에서 꺼낼 때 책장을 골라서 꽂을 수 있다 (비우면 원래 책장)
        long third = json(call(HttpMethod.POST, "/api/books/shelves", Map.of("name", "에세이"), token)).get("shelfId").asLong();
        call(HttpMethod.POST, "/api/books/completions/" + a + "/archive", null, token).andExpect(status().isOk());
        call(HttpMethod.DELETE, "/api/books/completions/" + a + "/archive?shelfId=" + third, null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.shelfId").value(third));
        call(HttpMethod.POST, "/api/books/completions/" + a + "/archive", null, token).andExpect(status().isOk());
        call(HttpMethod.DELETE, "/api/books/completions/" + a + "/archive", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shelfId").value(third)); // 비우면 보관하기 전 책장
        call(HttpMethod.POST, "/api/books/completions/" + a + "/archive", null, token).andExpect(status().isOk());
        call(HttpMethod.DELETE, "/api/books/completions/" + a + "/archive?shelfId=999999", null, token)
                .andExpect(status().isNotFound()); // 없는 책장
        call(HttpMethod.DELETE, "/api/books/completions/" + a + "/archive", null, token).andExpect(status().isOk());

        // 남의 책장은 쓸 수 없다
        String other = signup("shelves2@bookcanvas.com", "다른사람");
        long otherShelf = json(call(HttpMethod.GET, "/api/books/shelves", null, other)).get(0).get("shelfId").asLong();
        call(HttpMethod.PUT, "/api/books/completions/" + a + "/shelf", Map.of("shelfId", otherShelf), token).andExpect(status().isNotFound());
        call(HttpMethod.DELETE, "/api/books/shelves/" + first, null, other).andExpect(status().isNotFound());
        // 마지막 남은 책장은 지울 수 없다
        call(HttpMethod.DELETE, "/api/books/shelves/" + otherShelf, null, other)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_BOOKSHELF"));
    }

    @Test
    @DisplayName("도서 검색: 로그인 필수, 키가 없으면 503, 검색어가 비면 빈 목록 / 검색으로 고른 책은 ISBN·표지 저장, 수상한 표지 주소는 버림")
    void bookSearchAndAddWithMetadata() throws Exception {
        call(HttpMethod.GET, "/api/books/search?q=데미안", null, null).andExpect(status().isUnauthorized());

        String token = signup("search@bookcanvas.com", "검색주인");
        call(HttpMethod.GET, "/api/books/search?q=" + java.net.URLEncoder.encode("데미안", java.nio.charset.StandardCharsets.UTF_8), null, token)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("BOOK_SEARCH_UNAVAILABLE"));
        call(HttpMethod.GET, "/api/books/search?q=", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isEmpty());

        // 검색 결과에서 고른 책: ISBN·출판사·표지가 책 정보로 저장된다
        Map<String, Object> picked = new java.util.HashMap<>(shelfBook("데미안", "2026-10-01"));
        picked.put("isbn", "9788937460777");
        picked.put("publisher", "민음사");
        picked.put("coverImageUrl", "https://search1.kakaocdn.net/thumb/cover.jpg");
        call(HttpMethod.POST, "/api/books/completions", picked, token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.book.coverImageUrl").value("https://search1.kakaocdn.net/thumb/cover.jpg"));

        // 카카오 이미지 서버가 아닌 표지 주소는 서버가 나중에 내려받지 않도록 버린다
        Map<String, Object> sneaky = new java.util.HashMap<>(shelfBook("모모", "2026-10-02"));
        sneaky.put("isbn", "9788937460111");
        sneaky.put("coverImageUrl", "http://169.254.169.254/latest/meta-data");
        call(HttpMethod.POST, "/api/books/completions", sneaky, token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.book.coverImageUrl").doesNotExist());
    }

    @Test
    @DisplayName("책장: 추가 → 조회 → 수정 → 완독 인증(같은 항목이 ✓) → 인증 후 삭제·정보 변경 불가")
    void shelfLifecycle() throws Exception {
        String token = signup("shelf@bookcanvas.com", "책장주인");

        long id = json(call(HttpMethod.POST, "/api/books/completions", shelfBook("데미안", "2026-10-01"), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.certified").value(false))
                .andExpect(jsonPath("$.coverColor").value("#212529")))
                .get("completionId").asLong();

        // 같은 책 중복 추가 → 409
        call(HttpMethod.POST, "/api/books/completions", shelfBook("데미안", "2026-10-02"), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_ON_SHELF"));

        // 인증 전에는 날짜·표지 수정 가능
        call(HttpMethod.PUT, "/api/books/completions/" + id,
                Map.of("title", "데미안", "author", "헤르만 헤세", "completedAt", "2026-10-05", "coverColor", "#2F6B57"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").value("2026-10-05"))
                .andExpect(jsonPath("$.coverColor").value("#2F6B57"));

        // 완독 인증 → 새 항목이 아니라 같은 항목이 인증됨
        call(HttpMethod.POST, "/api/books/completion",
                Map.of("title", "데미안", "author", "헤르만 헤세", "completedAt", "2026-10-05", "reviewText", REVIEW), token)
                .andExpect(status().isCreated());

        JsonNode shelf = json(call(HttpMethod.GET, "/api/books/completions", null, token).andExpect(status().isOk()));
        assertThat(shelf).hasSize(1);
        assertThat(shelf.get(0).get("completionId").asLong()).isEqualTo(id);
        assertThat(shelf.get(0).get("certified").asBoolean()).isTrue();
        assertThat(shelf.get(0).get("coverColor").asText()).isEqualTo("#2F6B57");

        // 인증한 책: 삭제·날짜 변경 불가, 표지 색만 변경 가능
        call(HttpMethod.DELETE, "/api/books/completions/" + id, null, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CERTIFIED_BOOK_LOCKED"));
        call(HttpMethod.PUT, "/api/books/completions/" + id,
                Map.of("title", "데미안", "author", "헤르만 헤세", "completedAt", "2026-10-06"), token)
                .andExpect(status().isConflict());
        call(HttpMethod.PUT, "/api/books/completions/" + id,
                Map.of("title", "데미안", "author", "헤르만 헤세", "completedAt", "2026-10-05", "coverColor", "#E9ECEF"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverColor").value("#E9ECEF"));
    }

    @Test
    @DisplayName("책장: 계정마다 분리 — 다른 사람 책장 항목은 조회·수정·삭제할 수 없다(404)")
    void shelfIsolation() throws Exception {
        String tokenA = signup("a@bookcanvas.com", "독자A");
        String tokenB = signup("b@bookcanvas.com", "독자B");

        long idA = json(call(HttpMethod.POST, "/api/books/completions", shelfBook("모모", "2026-10-01"), tokenA)
                .andExpect(status().isCreated())).get("completionId").asLong();

        assertThat(json(call(HttpMethod.GET, "/api/books/completions", null, tokenB))).isEmpty();
        call(HttpMethod.DELETE, "/api/books/completions/" + idA, null, tokenB)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHELF_BOOK_NOT_FOUND"));
        assertThat(json(call(HttpMethod.GET, "/api/books/completions", null, tokenA))).hasSize(1);

        // 인증 안 한 책은 삭제 가능
        call(HttpMethod.DELETE, "/api/books/completions/" + idA, null, tokenA).andExpect(status().isNoContent());
        assertThat(json(call(HttpMethod.GET, "/api/books/completions", null, tokenA))).isEmpty();
    }

    @Test
    @DisplayName("책장 API 는 로그인 필수")
    void shelfRequiresAuth() throws Exception {
        call(HttpMethod.GET, "/api/books/completions", null, null).andExpect(status().isUnauthorized());
    }

    // ── 커뮤니티 ────────────────────────────────────────

    @Test
    @DisplayName("피드: 글쓰기 → 비로그인 조회 → 좋아요(멱등) → 댓글 → 남의 글 수정 403 → 수정 → 삭제")
    void quoteLifecycle() throws Exception {
        String writer = signup("writer@bookcanvas.com", "작성자");
        String reader = signup("reader@bookcanvas.com", "독자");

        long quoteId = json(call(HttpMethod.POST, "/api/quotes",
                Map.of("bookTitle", "어린 왕자", "author", "생텍쥐페리", "content", "가장 중요한 것은 눈에 보이지 않아.",
                        "tagName", "위로가 필요할 때"), writer)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.tags[0].name").value("위로가 필요할 때")))
                .get("quoteId").asLong();

        // 비로그인도 피드 조회 가능
        mockMvc.perform(get("/api/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quoteId").value(quoteId))
                .andExpect(jsonPath("$.content[0].mine").value(false))
                .andExpect(jsonPath("$.content[0].book.title").value("어린 왕자"));

        // 좋아요 두 번 눌러도 1 (멱등) → 취소하면 0
        call(HttpMethod.POST, "/api/quotes/" + quoteId + "/like", null, reader).andExpect(jsonPath("$.likeCount").value(1));
        call(HttpMethod.POST, "/api/quotes/" + quoteId + "/like", null, reader).andExpect(jsonPath("$.likeCount").value(1));
        call(HttpMethod.GET, "/api/quotes", null, reader)
                .andExpect(jsonPath("$.content[0].liked").value(true))
                .andExpect(jsonPath("$.content[0].likeCount").value(1));
        call(HttpMethod.DELETE, "/api/quotes/" + quoteId + "/like", null, reader)
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));

        // 댓글
        call(HttpMethod.POST, "/api/quotes/" + quoteId + "/comments", Map.of("content", "좋은 문장이네요"), reader)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.username").value("독자"));
        mockMvc.perform(get("/api/quotes/" + quoteId + "/comments"))
                .andExpect(jsonPath("$[0].content").value("좋은 문장이네요"));
        call(HttpMethod.GET, "/api/quotes", null, null).andExpect(jsonPath("$.content[0].commentCount").value(1));

        // 남의 글 수정·삭제 → 403
        Map<String, Object> edit = Map.of("bookTitle", "어린 왕자", "author", "생텍쥐페리",
                "content", "수정한 문장", "tagName", "여유가 필요할 때");
        call(HttpMethod.PUT, "/api/quotes/" + quoteId, edit, reader)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("QUOTE_FORBIDDEN"));

        // 내 글 수정 → 태그 교체 + edited
        call(HttpMethod.PUT, "/api/quotes/" + quoteId, edit, writer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정한 문장"))
                .andExpect(jsonPath("$.tags.length()").value(1))
                .andExpect(jsonPath("$.tags[0].name").value("여유가 필요할 때"))
                .andExpect(jsonPath("$.edited").value(true))
                .andExpect(jsonPath("$.commentCount").value(1));

        // 삭제 (댓글이 달려 있어도 삭제됨)
        call(HttpMethod.DELETE, "/api/quotes/" + quoteId, null, writer).andExpect(status().isNoContent());
        call(HttpMethod.GET, "/api/quotes", null, null).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("피드: 없는 태그로 글쓰기 400, 비로그인 글쓰기 401")
    void quoteValidation() throws Exception {
        String token = signup("tag@bookcanvas.com", "태그테스트");
        call(HttpMethod.POST, "/api/quotes",
                Map.of("bookTitle", "책", "author", "저자", "content", "문장", "tagName", "없는 태그"), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TAG"));
        call(HttpMethod.POST, "/api/quotes",
                Map.of("bookTitle", "책", "author", "저자", "content", "문장", "tagName", "위로가 필요할 때"), null)
                .andExpect(status().isUnauthorized());
    }

    // ── 계정 ────────────────────────────────────────────

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호가 틀리면 400, 성공하면 새 비밀번호로만 로그인된다")
    void changePassword() throws Exception {
        String token = signup("pw@bookcanvas.com", "비번변경");

        call(HttpMethod.POST, "/api/auth/password", Map.of("currentPassword", "wrong1234", "newPassword", "newpass456"), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
        call(HttpMethod.POST, "/api/auth/password", Map.of("currentPassword", "reading123", "newPassword", "reading123"), token)
                .andExpect(jsonPath("$.code").value("SAME_AS_CURRENT_PASSWORD"));
        call(HttpMethod.POST, "/api/auth/password", Map.of("currentPassword", "reading123", "newPassword", "short"), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));

        call(HttpMethod.POST, "/api/auth/password", Map.of("currentPassword", "reading123", "newPassword", "newpass456"), token)
                .andExpect(status().isNoContent());

        call(HttpMethod.POST, "/api/auth/login", Map.of("email", "pw@bookcanvas.com", "password", "reading123"), null)
                .andExpect(status().isUnauthorized());
        call(HttpMethod.POST, "/api/auth/login", Map.of("email", "pw@bookcanvas.com", "password", "newpass456"), null)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("시연용 간편 로그인: provider 별 고정 계정, 소셜 계정은 비밀번호 변경 불가, 미지원 provider 400")
    void demoLogin() throws Exception {
        JsonNode kakao1 = json(call(HttpMethod.POST, "/api/auth/demo-login", Map.of("provider", "kakao"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.provider").value("KAKAO"))
                .andExpect(jsonPath("$.user.username").value("moonreader_")));
        JsonNode kakao2 = json(call(HttpMethod.POST, "/api/auth/demo-login", Map.of("provider", "KAKAO"), null));
        assertThat(kakao1.at("/user/userId")).isEqualTo(kakao2.at("/user/userId")); // 같은 계정

        call(HttpMethod.POST, "/api/auth/demo-login", Map.of("provider", "google"), null)
                .andExpect(jsonPath("$.user.provider").value("GOOGLE"));
        call(HttpMethod.POST, "/api/auth/demo-login", Map.of("provider", "naver"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_PROVIDER"));

        call(HttpMethod.POST, "/api/auth/password",
                Map.of("currentPassword", "anything1", "newPassword", "newpass456"), kakao1.get("accessToken").asText())
                .andExpect(jsonPath("$.code").value("SOCIAL_ACCOUNT_PASSWORD"));
    }
}
