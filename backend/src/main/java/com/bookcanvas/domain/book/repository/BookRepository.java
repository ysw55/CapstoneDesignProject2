package com.bookcanvas.domain.book.repository;

import com.bookcanvas.domain.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Book 레포지토리
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    /** ISBN 이 없는 책(직접 입력)은 제목 + 저자가 같으면 같은 책으로 본다 */
    Optional<Book> findFirstByTitleAndAuthorOrderByIdAsc(String title, String author);
}
