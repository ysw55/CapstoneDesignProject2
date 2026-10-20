package com.bookcanvas.global.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Data JPA {@link Page}를 클라이언트 친화적인 형태로 감싸는 공통 페이징 응답 DTO
 *
 * <p>Spring의 {@code Page}를 그대로 직렬화하면 불필요한 내부 필드가 노출됩니다.
 * 이 DTO는 필요한 페이지 메타데이터만 추려서 일관된 응답 구조를 제공합니다.
 *
 * <p><b>응답 예시:</b>
 * <pre>{@code
 * {
 *   "content": [...],
 *   "pageNumber": 0,
 *   "pageSize": 10,
 *   "totalElements": 42,
 *   "totalPages": 5,
 *   "first": true,
 *   "last": false
 * }
 * }</pre>
 *
 * @param <T> 응답 데이터 타입 (예: QuoteResponse)
 */
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /**
     * Spring Data JPA {@link Page}로부터 {@link PageResponse}를 생성합니다.
     *
     * @param page Spring Data {@link Page} 객체
     * @param <T>  페이지 내용의 타입
     * @return 클라이언트 응답용 페이징 DTO
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
