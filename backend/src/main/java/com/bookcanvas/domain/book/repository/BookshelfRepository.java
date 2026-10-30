package com.bookcanvas.domain.book.repository;

import com.bookcanvas.domain.book.entity.Bookshelf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookshelfRepository extends JpaRepository<Bookshelf, Long> {

    /** 책장 탭 순서대로 */
    List<Bookshelf> findByUserIdOrderByPositionAscIdAsc(Long userId);

    /** 다른 사용자의 책장에 접근하지 못하도록 항상 userId 와 함께 조회 */
    Optional<Bookshelf> findByIdAndUserId(Long id, Long userId);
}
