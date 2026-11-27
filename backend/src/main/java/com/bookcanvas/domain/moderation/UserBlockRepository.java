package com.bookcanvas.domain.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Modifying
    @Query("DELETE FROM UserBlock b WHERE b.blocker.id = :blockerId AND b.blocked.id = :blockedId")
    void deleteByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    /** 내가 차단한 사용자 — 최근 차단한 순 */
    @Query("""
            SELECT b FROM UserBlock b
            JOIN FETCH b.blocked
            WHERE b.blocker.id = :blockerId
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    List<UserBlock> findByBlockerId(@Param("blockerId") Long blockerId);
}
