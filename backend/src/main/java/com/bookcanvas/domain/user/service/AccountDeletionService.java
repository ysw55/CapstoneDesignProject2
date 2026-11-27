package com.bookcanvas.domain.user.service;

import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회원탈퇴 — 계정과 그 계정이 만든 데이터를 모두 지운다 (되돌릴 수 없음).
 * 여러 사용자가 함께 쓰는 도서(Book)·태그(Tag)는 남긴다.
 *
 * <p><b>사용자를 참조하는 테이블을 새로 만들면 {@link #DELETIONS} 에도 추가해야 한다</b> (빠지면 FK 제약으로 탈퇴가 실패).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private static final String MY_QUOTES = "(SELECT q.id FROM Quote q WHERE q.user.id = :userId)";

    /** FK 제약에 걸리지 않도록 참조하는 쪽부터 지우는 순서 */
    private static final List<String> DELETIONS = List.of(
            // 내가 남긴 것 + 내 글에 다른 사람이 남긴 것
            "DELETE FROM QuoteLike x WHERE x.user.id = :userId OR x.quote.id IN " + MY_QUOTES,
            "DELETE FROM QuoteBookmark x WHERE x.user.id = :userId OR x.quote.id IN " + MY_QUOTES,
            "DELETE FROM QuoteComment x WHERE x.user.id = :userId OR x.quote.id IN " + MY_QUOTES,
            "DELETE FROM QuoteReport x WHERE x.reporter.id = :userId OR x.quote.id IN " + MY_QUOTES,
            "DELETE FROM UserBlock x WHERE x.blocker.id = :userId OR x.blocked.id = :userId",
            "DELETE FROM QuoteTagMap x WHERE x.quote.id IN " + MY_QUOTES,
            "DELETE FROM Quote x WHERE x.user.id = :userId",
            "DELETE FROM BookCompletion x WHERE x.user.id = :userId",
            "DELETE FROM Bookshelf x WHERE x.user.id = :userId",
            "DELETE FROM User x WHERE x.id = :userId");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    /**
     * @param password 이메일 계정의 본인 확인용 비밀번호 (소셜 계정은 비밀번호가 없어 확인하지 않음)
     * @throws BusinessException USER_NOT_FOUND, INVALID_CURRENT_PASSWORD
     */
    @Transactional
    public void withdraw(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.isSocialAccount() && (password == null || !passwordEncoder.matches(password, user.getPassword()))) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        DELETIONS.forEach(jpql -> entityManager.createQuery(jpql).setParameter("userId", userId).executeUpdate());
        entityManager.clear(); // 한꺼번에 지웠으므로 메모리에 남은 엔티티는 버린다
        log.info("[Auth] 회원탈퇴 - userId={}", userId);
    }
}
