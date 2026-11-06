package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.dto.BookSearchResponseDto;
import com.bookcanvas.domain.book.dto.BookSearchResultDto;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * 도서 검색 — 예스24 Open API 상품 검색 ({@code GET https://apis.yes24.com/v1/goods/itemList})
 *
 * <p>브라우저가 직접 부르지 않고 서버가 대신 불러서 API Key({@code X-Api-Key})를 숨긴다 (예스24 이용 조건).
 * 키가 없거나 호출이 실패하면 {@link ErrorCode#BOOK_SEARCH_UNAVAILABLE}(503) —
 * 화면은 이 경우 직접 입력으로 안내한다.
 *
 * <p><b>이용 조건:</b> 책 정보가 보이는 화면에 예스24 출처를 표기하고 상품 페이지({@code link})로 연결해야 한다.
 * 이 서비스는 {@code productUrl} 로 링크를 내려 주고, 출처 표기와 링크는 화면에서 한다.
 */
@Slf4j
@Service
public class BookSearchService {

    static final String BASE_URL = "https://apis.yes24.com";
    static final int PAGE_SIZE = 10;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_PAGE = 50;

    /** 검색 결과가 없을 때 예스24 가 주는 오류 코드 (404) — 오류가 아니라 빈 목록으로 돌려준다 */
    private static final String NO_RESULT_CODE = "SEARCH_001";

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 책등 이미지가 진짜 책등인지 가르는 기준: 높이 / 너비.
     * 예스24 의 진짜 책등은 400px 높이에 폭 11~64px(비율 6 이상)이고, 책등이 없는 책에 주는 공용 대체 이미지는
     * 280 x 400(비율 1.4)이다. 사이를 갈라 이 값보다 가늘고 긴 이미지만 책등으로 쓴다.
     */
    static final double MIN_SPINE_ASPECT = 2.0;
    private static final int IMAGE_CHECK_TIMEOUT_SECONDS = 5;

    private final RestClient restClient;
    private final String apiKey;
    private final Predicate<String> spineChecker;

    @Autowired
    public BookSearchService(
            RestClient.Builder builder,
            @Value("${app.book-search.yes24-api-key:}") String apiKey
    ) {
        this(builder
                .baseUrl(BASE_URL)
                .requestFactory(timeouts())
                .build(), apiKey, BookSearchService::downloadLooksLikeSpine);
    }

    /** 테스트에서 가짜 서버가 붙은 클라이언트와 가짜 책등 확인 함수를 넣기 위한 생성자 */
    BookSearchService(RestClient restClient, String apiKey, Predicate<String> spineChecker) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.spineChecker = spineChecker;
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
                    .uri(uri -> uri.path("/v1/goods/itemList")
                            .queryParam("query", "{q}")
                            .queryParam("category", "BOOK") // 국내도서
                            .queryParam("pageSize", PAGE_SIZE)
                            .queryParam("page", safePage)
                            .build(finalQuery))
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException e) {
            if (isNoResult(e)) {
                return new BookSearchResponseDto(List.of(), false);
            }
            // 키 오류(401), 호출 한도(429) 등 — 사용자에게는 '지금은 검색 불가'
            log.warn("[BookSearch] 예스24 도서 검색 실패 - {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.BOOK_SEARCH_UNAVAILABLE);
        } catch (RestClientException e) {
            log.warn("[BookSearch] 예스24 도서 검색 실패 - {}", e.getMessage());
            throw new BusinessException(ErrorCode.BOOK_SEARCH_UNAVAILABLE);
        }
        BookSearchResponseDto response = toResponse(body);
        return new BookSearchResponseDto(keepOnlyRealSpines(response.results()), response.hasMore());
    }

    /**
     * 책등 주소가 있는 결과의 이미지를 병렬로 내려받아(작은 파일) 진짜 책등이 아닌 것은 주소를 비운다.
     * 예스24 는 책등이 없는 책에도 대체 이미지 주소를 주기 때문에, 주소만으로는 구분할 수 없다.
     * 확인이 시간 안에 끝나지 않거나 실패하면 그 책은 책등 없음으로 본다 (화면은 색 책등으로 대신한다).
     */
    private List<BookSearchResultDto> keepOnlyRealSpines(List<BookSearchResultDto> results) {
        List<CompletableFuture<Boolean>> checks = results.stream()
                .map(r -> r.spineImageUrl() == null
                        ? CompletableFuture.completedFuture(false)
                        : CompletableFuture.supplyAsync(() -> spineChecker.test(r.spineImageUrl())))
                .toList();
        try {
            CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
                    .get(IMAGE_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            // 일부가 늦거나 실패해도 아래에서 책별로 처리한다
        }

        List<BookSearchResultDto> checked = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            BookSearchResultDto r = results.get(i);
            CompletableFuture<Boolean> check = checks.get(i);
            boolean real = check.isDone() && !check.isCompletedExceptionally() && check.join();
            checked.add(real || r.spineImageUrl() == null ? r : new BookSearchResultDto(
                    r.title(), r.author(), r.publisher(), r.isbn(), r.coverImageUrl(), null, r.productUrl(), r.publishedAt()));
        }
        return checked;
    }

    /** 이미지를 내려받아 크기만 읽고(디코딩하지 않음) 진짜 책등 모양인지 본다 */
    static boolean downloadLooksLikeSpine(String url) {
        if (!isTrustedCoverUrl(url)) return false;
        try {
            URLConnection connection = URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(3_000);
            connection.setReadTimeout(4_000);
            try (InputStream in = connection.getInputStream();
                 ImageInputStream imageIn = ImageIO.createImageInputStream(in)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageIn);
                if (!readers.hasNext()) return false;
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageIn);
                    return looksLikeSpine(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        } catch (Exception e) {
            log.debug("[BookSearch] 책등 이미지 확인 실패 - {}", e.getMessage());
            return false;
        }
    }

    static boolean looksLikeSpine(int width, int height) {
        return width > 0 && height > 0 && (double) height / width >= MIN_SPINE_ASPECT;
    }

    private static boolean isNoResult(HttpClientErrorException e) {
        if (e.getStatusCode().value() != 404) return false;
        try {
            return NO_RESULT_CODE.equals(JSON.readTree(e.getResponseBodyAsString()).path("errorCode").asText(""));
        } catch (Exception ignored) {
            return false;
        }
    }

    private BookSearchResponseDto toResponse(JsonNode body) {
        if (body == null || !body.path("success").asBoolean(false)) {
            log.warn("[BookSearch] 예스24 응답이 성공이 아님 - {}", body == null ? "빈 응답" : body.path("errorCode").asText());
            throw new BusinessException(ErrorCode.BOOK_SEARCH_UNAVAILABLE);
        }

        JsonNode data = body.path("data");
        List<BookSearchResultDto> results = new ArrayList<>();
        for (JsonNode item : data.path("items")) {
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) continue;
            long itemId = item.path("itemId").asLong(0);
            results.add(new BookSearchResultDto(
                    title,
                    item.path("author").asText("").trim(),
                    blankToNull(item.path("publisher").asText("")),
                    pickIsbn(item.path("isbn13").asText(""), item.path("isbn10").asText("")),
                    registeredImage(item.path("cover").asText(""), itemId),
                    registeredImage(item.path("sideCover").asText(""), itemId),
                    trustedProductUrl(item.path("link").asText("")),
                    dateOnly(item.path("publishDate").asText(""))
            ));
        }
        boolean hasMore = (long) data.path("currentPage").asInt(1) * data.path("pageSize").asInt(PAGE_SIZE)
                < data.path("totalCount").asLong(0);
        return new BookSearchResponseDto(results, hasMore);
    }

    static String pickIsbn(String isbn13, String isbn10) {
        if (StringUtils.hasText(isbn13)) return isbn13.trim();
        return StringUtils.hasText(isbn10) ? isbn10.trim() : null;
    }

    /**
     * 이미지가 실제로 등록된 책의 것인지 거른다. 예스24 는 이미지가 없는 책에도 대체 이미지 URL 을 주고
     * 응답만으로는 구분할 수 없다고 안내한다. 등록된 이미지는 주소에 상품 번호({@code /goods/{itemId}/...})가
     * 들어 있으므로, 그렇지 않은 주소(공용 대체 이미지)는 이미지가 없는 것으로 본다.
     */
    static String registeredImage(String url, long itemId) {
        String u = url.trim();
        if (u.isEmpty() || itemId <= 0 || !u.contains("/goods/" + itemId)) return null;
        return u.startsWith("http://") ? "https://" + u.substring("http://".length()) : u;
    }

    private static String trustedProductUrl(String url) {
        return isTrustedProductUrl(url) ? url.trim() : null;
    }

    private static String dateOnly(String date) {
        return date.length() >= 10 ? date.substring(0, 10) : null;
    }

    private static String blankToNull(String s) {
        return s.isBlank() ? null : s.trim();
    }

    /** 책 추가로 받은 이미지 URL 이 예스24 · 카카오 이미지 서버 주소인지 (임의 주소를 서버가 나중에 내려받지 않도록) */
    public static boolean isTrustedCoverUrl(String url) {
        String host = httpsHost(url);
        return host != null && (host.equals("image.yes24.com") || host.endsWith(".kakaocdn.net") || host.endsWith(".daumcdn.net"));
    }

    /** 예스24 상품 페이지 주소인지 */
    public static boolean isTrustedProductUrl(String url) {
        String host = httpsHost(url);
        return host != null && (host.equals("www.yes24.com") || host.equals("m.yes24.com") || host.equals("yes24.com"));
    }

    private static String httpsHost(String url) {
        if (!StringUtils.hasText(url)) return null;
        try {
            URI uri = URI.create(url.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) ? uri.getHost() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
