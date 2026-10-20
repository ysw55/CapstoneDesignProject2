package com.bookcanvas.domain.tag.repository;

import com.bookcanvas.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Tag 레포지토리
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 주어진 ID 목록 중 실제로 DB에 존재하는 태그의 수를 반환합니다.
     * 존재하지 않는 태그 ID가 요청에 포함되었는지 검증하는 데 사용됩니다.
     *
     * @param ids 검증할 태그 ID 목록
     * @return 실제로 존재하는 태그의 수
     */
    long countByIdIn(List<Long> ids);

    Optional<Tag> findByName(String name);
}
