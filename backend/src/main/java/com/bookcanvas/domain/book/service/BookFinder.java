package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 책장·완독 인증·커뮤니티 글쓰기에서 공통으로 쓰는 '도서 찾기 또는 등록'
 *
 * <p>ISBN 이 있으면 ISBN 으로, 없으면(직접 입력) 제목 + 저자로 같은 책을 찾습니다.
 * 외부 도서 검색 API 를 붙이면 ISBN 기반으로 자연스럽게 중복이 정리됩니다.
 */
@Component
@RequiredArgsConstructor
public class BookFinder {

    private final BookRepository bookRepository;

    @Transactional
    public Book findOrCreate(String title, String author) {
        return findOrCreate(title, author, null, null, null);
    }

    @Transactional
    public Book findOrCreate(String title, String author, String isbn, String publisher, String coverImageUrl) {
        String t = title.trim();
        String a = author.trim();
        String i = StringUtils.hasText(isbn) ? isbn.trim() : null;

        return (i != null
                ? bookRepository.findByIsbn(i)
                : bookRepository.findFirstByTitleAndAuthorOrderByIdAsc(t, a))
                .orElseGet(() -> bookRepository.save(Book.builder()
                        .title(t)
                        .author(a)
                        .isbn(i)
                        .publisher(publisher)
                        .coverImageUrl(coverImageUrl)
                        .build()));
    }
}
