package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.dto.BookSearchResponseDto;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

/** 카카오 책 검색 응답 변환 (가짜 서버 사용 — 실제 카카오를 부르지 않는다) */
class BookSearchServiceTest {

    private static final String KAKAO_RESPONSE = """
            {
              "meta": { "is_end": false, "pageable_count": 42, "total_count": 120 },
              "documents": [
                {
                  "title": "데미안",
                  "authors": ["헤르만 헤세"],
                  "publisher": "민음사",
                  "isbn": "8937460777 9788937460777",
                  "datetime": "2000-12-20T00:00:00.000+09:00",
                  "thumbnail": "http://search1.kakaocdn.net/thumb/R120x174.q85/?fname=cover"
                },
                {
                  "title": "공저 도서",
                  "authors": ["가", "나"],
                  "publisher": "",
                  "isbn": "1234567890",
                  "datetime": "",
                  "thumbnail": ""
                },
                { "title": "  ", "authors": [], "isbn": "", "thumbnail": "" }
              ]
            }
            """;

    private MockRestServiceServer server;

    private BookSearchService service(String apiKey) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BookSearchService.BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        return new BookSearchService(builder.build(), apiKey);
    }

    @Test
    @DisplayName("카카오 응답을 책 목록으로 바꾼다: ISBN-13 우선, http→https, 저자 합치기, 빈 값은 null")
    void mapsKakaoResponse() {
        BookSearchService service = service("test-key");
        server.expect(requestTo(startsWith("https://dapi.kakao.com/v3/search/book")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andExpect(queryParam("query", "%EB%8D%B0%EB%AF%B8%EC%95%88")) // "데미안" 이 UTF-8 로 인코딩돼 나간다
                .andExpect(queryParam("size", "10"))
                .andExpect(queryParam("page", "1"))
                .andRespond(withSuccess(KAKAO_RESPONSE, MediaType.APPLICATION_JSON));

        BookSearchResponseDto result = service.search("  데미안  ", 1);

        server.verify();
        assertThat(result.hasMore()).isTrue();
        assertThat(result.results()).hasSize(2); // 제목이 빈 항목은 버린다

        var first = result.results().get(0);
        assertThat(first.title()).isEqualTo("데미안");
        assertThat(first.author()).isEqualTo("헤르만 헤세");
        assertThat(first.publisher()).isEqualTo("민음사");
        assertThat(first.isbn()).isEqualTo("9788937460777");
        assertThat(first.coverImageUrl()).startsWith("https://search1.kakaocdn.net/");
        assertThat(first.publishedAt()).isEqualTo("2000-12-20");

        var second = result.results().get(1);
        assertThat(second.author()).isEqualTo("가, 나");
        assertThat(second.publisher()).isNull();
        assertThat(second.isbn()).isEqualTo("1234567890"); // ISBN-13 이 없으면 ISBN-10
        assertThat(second.coverImageUrl()).isNull();
        assertThat(second.publishedAt()).isNull();
    }

    @Test
    @DisplayName("검색어가 비면 카카오를 부르지 않고 빈 목록, page 는 1~50 으로 보정")
    void emptyQueryAndPageClamp() {
        BookSearchService service = service("test-key");
        assertThat(service.search("   ", 1).results()).isEmpty();

        server.expect(requestTo(startsWith("https://dapi.kakao.com/v3/search/book")))
                .andExpect(queryParam("page", "50"))
                .andRespond(withSuccess("{\"meta\":{\"is_end\":true},\"documents\":[]}", MediaType.APPLICATION_JSON));
        assertThat(service.search("책", 999).hasMore()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("키가 없거나 카카오가 오류를 주면 '검색 불가' 로 응답한다")
    void unavailable() {
        assertThatThrownBy(() -> service("").search("데미안", 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_UNAVAILABLE));

        BookSearchService service = service("bad-key");
        server.expect(requestTo(startsWith("https://dapi.kakao.com/v3/search/book")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> service.search("데미안", 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_UNAVAILABLE));
    }

    @Test
    @DisplayName("표지 URL 은 카카오 이미지 서버(https)만 믿는다")
    void trustedCoverUrl() {
        assertThat(BookSearchService.isTrustedCoverUrl("https://search1.kakaocdn.net/a.jpg")).isTrue();
        assertThat(BookSearchService.isTrustedCoverUrl("https://t1.daumcdn.net/b.png")).isTrue();
        assertThat(BookSearchService.isTrustedCoverUrl("http://search1.kakaocdn.net/a.jpg")).isFalse();
        assertThat(BookSearchService.isTrustedCoverUrl("https://evil.example.com/a.jpg")).isFalse();
        assertThat(BookSearchService.isTrustedCoverUrl("https://kakaocdn.net.evil.com/a.jpg")).isFalse();
        assertThat(BookSearchService.isTrustedCoverUrl("http://169.254.169.254/latest/meta-data")).isFalse();
        assertThat(BookSearchService.isTrustedCoverUrl("not a url")).isFalse();
        assertThat(BookSearchService.isTrustedCoverUrl(null)).isFalse();
    }
}
