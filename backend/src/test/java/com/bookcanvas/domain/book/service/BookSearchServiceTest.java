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
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** 예스24 도서 검색 응답 변환 (가짜 서버 사용 — 실제 예스24를 부르지 않는다) */
class BookSearchServiceTest {

    // 예스24 개발자센터 문서(goods/itemList)의 응답 형식
    private static final String YES24_RESPONSE = """
            {
              "success": true, "message": "성공", "errorCode": null,
              "data": {
                "currentPage": 1, "pageSize": 10, "totalCount": 25,
                "items": [
                  {
                    "itemId": 12345678, "title": "클린 코드", "author": "로버트 C. 마틴", "publisher": "인사이트",
                    "isbn10": "8966260950", "isbn13": "9788966260959", "publishDate": "2013-12-24",
                    "cover": "https://image.yes24.com/goods/12345678/L",
                    "sideCover": "https://image.yes24.com/goods/12345678/SIDE/L",
                    "backCover": "https://image.yes24.com/goods/12345678/BACK/L",
                    "link": "https://www.yes24.com/product/goods/12345678"
                  },
                  {
                    "itemId": 777, "title": "이미지 없는 책", "author": "가, 나", "publisher": "",
                    "isbn10": "1234567890", "isbn13": "", "publishDate": "",
                    "cover": "https://image.yes24.com/sysimage/noimg/L.png",
                    "sideCover": "https://image.yes24.com/sysimage/noimg/SIDE.png",
                    "link": "https://evil.example.com/steal"
                  },
                  { "itemId": 1, "title": "  ", "author": "", "cover": "", "sideCover": "", "link": "" }
                ]
              }
            }
            """;

    private MockRestServiceServer server;

    private BookSearchService service(String apiKey) {
        return service(apiKey, url -> true);
    }

    /** spineChecker: 책등 이미지를 실제로 내려받는 대신 쓰는 가짜 판정 */
    private BookSearchService service(String apiKey, java.util.function.Predicate<String> spineChecker) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BookSearchService.BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        return new BookSearchService(builder.build(), apiKey, spineChecker);
    }

    @Test
    @DisplayName("예스24 응답을 책 목록으로 바꾼다: 책등·표지·상품 링크, ISBN-13 우선, 등록 안 된 이미지와 수상한 링크는 버림")
    void mapsYes24Response() {
        BookSearchService service = service("test-key");
        server.expect(requestTo(startsWith("https://apis.yes24.com/v1/goods/itemList")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Api-Key", "test-key"))
                .andExpect(queryParam("query", "%ED%81%B4%EB%A6%B0%20%EC%BD%94%EB%93%9C")) // "클린 코드" UTF-8 인코딩
                .andExpect(queryParam("category", "BOOK"))
                .andExpect(queryParam("pageSize", "10"))
                .andExpect(queryParam("page", "1"))
                .andRespond(withSuccess(YES24_RESPONSE, MediaType.APPLICATION_JSON));

        BookSearchResponseDto result = service.search("  클린 코드  ", 1);

        server.verify();
        assertThat(result.hasMore()).isTrue(); // 1 x 10 < 25
        assertThat(result.results()).hasSize(2); // 제목이 빈 항목은 버린다

        var first = result.results().get(0);
        assertThat(first.title()).isEqualTo("클린 코드");
        assertThat(first.author()).isEqualTo("로버트 C. 마틴");
        assertThat(first.publisher()).isEqualTo("인사이트");
        assertThat(first.isbn()).isEqualTo("9788966260959");
        assertThat(first.coverImageUrl()).isEqualTo("https://image.yes24.com/goods/12345678/L");
        assertThat(first.spineImageUrl()).isEqualTo("https://image.yes24.com/goods/12345678/SIDE/L");
        assertThat(first.productUrl()).isEqualTo("https://www.yes24.com/product/goods/12345678");
        assertThat(first.publishedAt()).isEqualTo("2013-12-24");

        var second = result.results().get(1);
        assertThat(second.isbn()).isEqualTo("1234567890"); // ISBN-13 이 없으면 ISBN-10
        assertThat(second.publisher()).isNull();
        assertThat(second.coverImageUrl()).isNull();  // 대체 이미지는 '이미지 없음' 으로 본다
        assertThat(second.spineImageUrl()).isNull();
        assertThat(second.productUrl()).isNull();      // 예스24 가 아닌 주소는 버린다
        assertThat(second.publishedAt()).isNull();
    }

    @Test
    @DisplayName("진짜 책등이 아닌 이미지(공용 대체 이미지)는 책등 주소를 비운다, 확인 중 오류가 나도 비운다")
    void dropsNonSpineImages() {
        // 예스24 는 책등이 없는 책에도 같은 모양의 주소를 준다 → 이미지를 직접 보고 가른다
        BookSearchService service = service("test-key", url -> url.contains("/goods/12345678/"));
        server.expect(requestTo(startsWith("https://apis.yes24.com/v1/goods/itemList")))
                .andRespond(withSuccess("""
                        {"success":true,"data":{"currentPage":1,"pageSize":10,"totalCount":2,"items":[
                          {"itemId":12345678,"title":"책등 있는 책","author":"가","cover":"https://image.yes24.com/goods/12345678/L",
                           "sideCover":"https://image.yes24.com/goods/12345678/SIDE/L","link":"https://www.yes24.com/product/goods/12345678"},
                          {"itemId":999,"title":"책등 없는 책","author":"나","cover":"https://image.yes24.com/goods/999/L",
                           "sideCover":"https://image.yes24.com/goods/999/SIDE/L","link":"https://www.yes24.com/product/goods/999"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        var results = service.search("책", 1).results();

        assertThat(results.get(0).spineImageUrl()).isEqualTo("https://image.yes24.com/goods/12345678/SIDE/L");
        assertThat(results.get(1).spineImageUrl()).isNull();              // 대체 이미지 → 책등 없음
        assertThat(results.get(1).coverImageUrl()).isNotNull();            // 앞표지는 그대로
        assertThat(results.get(1).productUrl()).isNotNull();

        BookSearchService failing = service("test-key", url -> { throw new RuntimeException("확인 실패"); });
        server.expect(requestTo(startsWith("https://apis.yes24.com/v1/goods/itemList")))
                .andRespond(withSuccess("""
                        {"success":true,"data":{"currentPage":1,"pageSize":10,"totalCount":1,"items":[
                          {"itemId":5,"title":"책","author":"가","sideCover":"https://image.yes24.com/goods/5/SIDE/L"}]}}
                        """, MediaType.APPLICATION_JSON));
        assertThat(failing.search("책", 1).results().get(0).spineImageUrl()).isNull();
    }

    @Test
    @DisplayName("책등 모양 판정: 진짜 책등(64x400 등)은 통과, 공용 대체 이미지(280x400)·가로로 넓은 이미지는 탈락")
    void spineShape() {
        assertThat(BookSearchService.looksLikeSpine(64, 400)).isTrue();   // 가장 두꺼운 실제 책등
        assertThat(BookSearchService.looksLikeSpine(11, 400)).isTrue();   // 가장 얇은 실제 책등
        assertThat(BookSearchService.looksLikeSpine(280, 400)).isFalse(); // 예스24 공용 대체 이미지
        assertThat(BookSearchService.looksLikeSpine(400, 400)).isFalse();
        assertThat(BookSearchService.looksLikeSpine(0, 400)).isFalse();
        assertThat(BookSearchService.looksLikeSpine(30, 0)).isFalse();
    }

    @Test
    @DisplayName("마지막 페이지면 hasMore 가 false")
    void lastPage() {
        BookSearchService service = service("test-key");
        server.expect(requestTo(startsWith("https://apis.yes24.com/v1/goods/itemList")))
                .andExpect(queryParam("page", "3"))
                .andRespond(withSuccess("""
                        {"success":true,"data":{"currentPage":3,"pageSize":10,"totalCount":25,"items":[]}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.search("책", 3).hasMore()).isFalse(); // 3 x 10 >= 25
        server.verify();
    }

    @Test
    @DisplayName("검색어가 비면 호출하지 않고 빈 목록, page 는 1~50 으로 보정")
    void emptyQueryAndPageClamp() {
        BookSearchService service = service("test-key");
        assertThat(service.search("   ", 1).results()).isEmpty();

        server.expect(requestTo(startsWith("https://apis.yes24.com/v1/goods/itemList")))
                .andExpect(queryParam("page", "50"))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"items\":[]}}", MediaType.APPLICATION_JSON));
        service.search("책", 999);
        server.verify();
    }

    @Test
    @DisplayName("검색 결과가 없으면(404 SEARCH_001) 오류가 아니라 빈 목록")
    void noResultIsNotAnError() {
        BookSearchService service = service("test-key");
        server.expect(requestTo(startsWith("https://apis.yes24.com/v1/goods/itemList")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"errorCode\":\"SEARCH_001\",\"message\":\"검색 결과 없음\",\"data\":null}"));

        BookSearchResponseDto result = service.search("zzzzzqqqq", 1);

        assertThat(result.results()).isEmpty();
        assertThat(result.hasMore()).isFalse();
    }

    @Test
    @DisplayName("키가 없거나 예스24가 오류(401·429·503)를 주거나 실패 응답이면 '검색 불가' 로 응답한다")
    void unavailable() {
        assertThatThrownBy(() -> service("").search("데미안", 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_UNAVAILABLE));

        for (HttpStatus status : new HttpStatus[]{HttpStatus.UNAUTHORIZED, HttpStatus.TOO_MANY_REQUESTS, HttpStatus.SERVICE_UNAVAILABLE}) {
            BookSearchService service = service("bad-key");
            server.expect(requestTo(startsWith("https://apis.yes24.com/v1/goods/itemList"))).andRespond(withStatus(status));
            assertThatThrownBy(() -> service.search("데미안", 1))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_UNAVAILABLE));
        }

        BookSearchService service = service("test-key");
        server.expect(requestTo(startsWith("https://apis.yes24.com/v1/goods/itemList")))
                .andRespond(withSuccess("{\"success\":false,\"errorCode\":\"UPSTREAM_UNAVAILABLE\",\"data\":null}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.search("데미안", 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_UNAVAILABLE));
    }

    @Test
    @DisplayName("이미지 URL 은 예스24·카카오 이미지 서버(https)만, 상품 링크는 예스24 주소만 믿는다")
    void trustedUrls() {
        assertThat(BookSearchService.isTrustedCoverUrl("https://image.yes24.com/goods/1/SIDE/L")).isTrue();
        assertThat(BookSearchService.isTrustedCoverUrl("https://search1.kakaocdn.net/a.jpg")).isTrue();
        assertThat(BookSearchService.isTrustedCoverUrl("http://image.yes24.com/goods/1/L")).isFalse();
        assertThat(BookSearchService.isTrustedCoverUrl("https://image.yes24.com.evil.com/a.jpg")).isFalse();
        assertThat(BookSearchService.isTrustedCoverUrl("https://evil.example.com/a.jpg")).isFalse();
        assertThat(BookSearchService.isTrustedCoverUrl("http://169.254.169.254/latest/meta-data")).isFalse();
        assertThat(BookSearchService.isTrustedCoverUrl(null)).isFalse();

        assertThat(BookSearchService.isTrustedProductUrl("https://www.yes24.com/product/goods/1")).isTrue();
        assertThat(BookSearchService.isTrustedProductUrl("http://www.yes24.com/product/goods/1")).isFalse();
        assertThat(BookSearchService.isTrustedProductUrl("https://www.yes24.com.evil.com/x")).isFalse();
        assertThat(BookSearchService.isTrustedProductUrl("javascript:alert(1)")).isFalse();
    }

    @Test
    @DisplayName("등록된 이미지만 쓴다: 주소에 상품 번호가 있어야 하고, http 는 https 로")
    void registeredImageOnly() {
        assertThat(BookSearchService.registeredImage("https://image.yes24.com/goods/42/SIDE/L", 42)).isNotNull();
        assertThat(BookSearchService.registeredImage("http://image.yes24.com/goods/42/L", 42)).startsWith("https://");
        assertThat(BookSearchService.registeredImage("https://image.yes24.com/sysimage/noimg.png", 42)).isNull();
        assertThat(BookSearchService.registeredImage("https://image.yes24.com/goods/43/L", 42)).isNull();
        assertThat(BookSearchService.registeredImage("", 42)).isNull();
        assertThat(BookSearchService.registeredImage("https://image.yes24.com/goods/42/L", 0)).isNull();
    }
}
