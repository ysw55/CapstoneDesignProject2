package com.bookcanvas.domain.user.repository;

import com.bookcanvas.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * User 레포지토리
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 같은 사용자의 요청이 동시에 와도 한 번에 하나씩 처리하도록 행을 잠그고 조회 (기본 책장 중복 생성 방지) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
