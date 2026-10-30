package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.dto.BookSearchResponseDto;
import com.bookcanvas.domain.book.dto.BookSearchResultDto;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 도서 검색 — 카카오 책 검색 API ({@code GET https://dapi.kakao.com/v3/search/book})
 *
 * <p>브라우저가 직접 부르지 않고 서버가 대신 불러서 REST API 키를 숨긴다.
 * 키가 없거나 카카오 호출이 실패하면 {@link ErrorCode#BOOK_SEARCH_UNAVAILABLE}(503) —
 * 화면은 이 경우 직접 입력으로 안내한다.
 */
@Slf4j
@Service
public class BookSearchService {

    static final String BASE_URL = "https://dapi.kakao.com";
    static final int PAGE_SIZE = 10;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_PAGE = 50; // 카카오 API 의 page 상한

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public BookSearchService(
            RestClient.Builder builder,
            @Value("${app.book-search.kakao-rest-api-key:}") String apiKey
    ) {
        this(builder
                .baseUrl(BASE_URL)
                .requestFactory(timeouts())
                .build(), apiKey);
    }

    /** 테스트에서 가짜 서버가 붙은 클라이언트를 넣기 위한 생성자 */
    BookSearchService(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    private static JdkClientHttpRequestFactory timeouts() {
        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    /** 검색 기능을 쓸 수 있는지 (키가 설정돼 있는지) */
    public boolean isAvailable() {
        return StringUtils.hasText(apiKey);
    }

    public BookSearchResponseDto search(String query, int page) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return new BookSearchResponseDto(List.of(), false);
        }
        if (q.length() > MAX_QUERY_LENGTH) {
            q = q.substring(0, MAX_QUERY_LENGTH);
        }
        if (!isAvailable()) {
            throw new BusinessException(ErrorCode.BOOK_SEARCH_UNAVAILABLE);
        }

        String finalQuery = q;
        int safePage = Math.min(Math.max(page, 1), MAX_PAGE);
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/v3/search/book")
                            .queryParam("query", "{q}")
                            .queryParam("size", PAGE_SIZE)
                            .queryParam("page", safePage)
                            .build(finalQuery))
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            // 키 오류(401), 호출 한도(429), 네트워크 문제 모두 사용자에게는 '지금은 검색 불가'
            log.warn("[BookSearch] 카카오 도서 검색 실패 - {}", e.getMessage());
            throw new BusinessException(ErrorCode.BOOK_SEARCH_UNAVAILABLE);
        }
        return toResponse(body);
    }

    private BookSearchResponseDto toResponse(JsonNode body) {
        List<BookSearchResultDto> results = new ArrayList<>();
        if (body != null) {
            for (JsonNode doc : body.path("documents")) {
                String title = doc.path("title").asText("").trim();
                if (title.isEmpty()) continue;
                results.add(new BookSearchResultDto(
                        title,
                        joinAuthors(doc.path("authors")),
                        blankToNull(doc.path("publisher").asText("")),
                        pickIsbn(doc.path("isbn").asText("")),
                        httpsOrNull(doc.path("thumbnail").asText("")),
                        dateOnly(doc.path("datetime").asText(""))
                ));
            }
        }
        boolean hasMore = body != null && !body.path("meta").path("is_end").asBoolean(true);
        return new BookSearchResponseDto(results, hasMore);
    }

    private static String joinAuthors(JsonNode authors) {
        List<String> names = new ArrayList<>();
        for (JsonNode a : authors) {
            String name = a.asText("").trim();
            if (!name.isEmpty()) names.add(name);
        }
        return String.join(", ", names);
    }

    /** 카카오는 "ISBN10 ISBN13" 을 공백으로 이어서 준다 → ISBN-13 우선 */
    static String pickIsbn(String raw) {
        String isbn10 = null;
        for (String part : raw.trim().split("\\s+")) {
            if (part.length() == 13) return part;
            if (part.length() == 10) isbn10 = part;
        }
        return isbn10;
    }

    private static String httpsOrNull(String url) {
        String u = url.trim();
        if (u.isEmpty()) return null;
        return u.startsWith("http://") ? "https://" + u.substring("http://".length()) : u;
    }

    /** "2000-12-20T00:00:00.000+09:00" → "2000-12-20" */
    private static String dateOnly(String datetime) {
        return datetime.length() >= 10 ? datetime.substring(0, 10) : null;
    }

    private static String blankToNull(String s) {
        return s.isBlank() ? null : s.trim();
    }

    /** 책 추가로 받은 표지 URL 이 카카오 이미지 서버 주소인지 (임의 주소를 서버가 나중에 내려받지 않도록) */
    public static boolean isTrustedCoverUrl(String url) {
        if (!StringUtils.hasText(url)) return false;
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && host != null
                    && (host.endsWith(".kakaocdn.net") || host.endsWith(".daumcdn.net"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
