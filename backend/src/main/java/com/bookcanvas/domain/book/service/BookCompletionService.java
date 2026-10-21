package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.dto.BookCompletionRequestDto;
import com.bookcanvas.domain.book.dto.BookCompletionResponseDto;
import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.entity.BookCompletion;
import com.bookcanvas.domain.book.repository.BookCompletionRepository;
import com.bookcanvas.domain.book.service.BookCoverColorExtractorService.ColorExtractionException;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 완독 인증(독후감 제출) 비즈니스 로직 서비스
 *
 * <p><b>완독 인증 흐름:</b>
 * <pre>
 *   클라이언트 요청 (JWT 로 사용자 식별)
 *       ↓
 *   1. 독후감 200자 이상 검증 (DTO @Size 1차 + 서비스 2차, 공백 제외 기준)
 *       ↓
 *   2. 완독 날짜 검증 (미래 날짜 불가, 없으면 오늘)
 *       ↓
 *   3. 사용자 조회 / 도서 조회 또는 신규 등록 (ISBN → 제목+저자 순)
 *       ↓
 *   4. 중복 인증 확인 (같은 책은 한 번만)
 *       ↓
 *   5. 도서 표지 → 프로필 스킨 색상 추출 (실패 시 기본 스킨)
 *       ↓
 *   6. User 에 스킨 적용 + 포인트 적립
 *      책장에 이미 꽂아 둔 책이면 그 항목을 '인증됨'으로, 없으면 인증된 상태로 새로 꽂음
 *       ↓
 *   7. 결과 반환
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookCompletionService {

    /** 완독 인증에 필요한 독후감 최소 글자 수 */
    public static final int MINIMUM_REVIEW_LENGTH = 200;

    /** 완독 인증 1회당 지급 포인트 */
    public static final int COMPLETION_REWARD_POINT = 100;

    /** 도서 표지 이미지가 없을 때 지급하는 기본 스킨 (Book Canvas 딥그린 테마) */
    static final String DEFAULT_BACKGROUND_COLOR = "#1B4D3E";
    static final String DEFAULT_BORDER_COLOR = "#133A2F";

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final BookFinder bookFinder;
    private final BookCompletionRepository bookCompletionRepository;
    private final BookCoverColorExtractorService colorExtractorService;

    /**
     * 완독 인증을 처리하고 완독 책장에 추가한 뒤, 보상 스킨과 포인트를 지급합니다.
     *
     * @param userId  JWT 에서 꺼낸 로그인 사용자 ID
     * @param request 도서 정보 + 독후감
     * @throws BusinessException REVIEW_TOO_SHORT, INVALID_COMPLETION_DATE, USER_NOT_FOUND, ALREADY_COMPLETED
     */
    @Transactional
    public BookCompletionResponseDto certifyCompletion(Long userId, BookCompletionRequestDto request) {
        log.info("[BookCompletion] 완독 인증 요청 - userId={}, title={}", userId, request.title());

        // 1~2. 입력값 검증 (DB 조회 전에 먼저 걸러냄)
        String reviewText = validateReviewLength(request.reviewText());
        LocalDate completedAt = resolveCompletedAt(request.completedAt());

        // 3. 사용자 / 도서
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Book book = bookFinder.findOrCreate(
                request.title(), request.author(), request.isbn(), request.publisher(), request.coverImageUrl());

        // 4. 책장 항목 확인 — 이미 인증했으면 거절, 책장에만 있던 책이면 그 항목을 인증
        BookCompletion completion = bookCompletionRepository.findByUserIdAndBookId(userId, book.getId())
                .orElseGet(() -> BookCompletion.builder()
                        .user(user)
                        .book(book)
                        .completedAt(completedAt)
                        .coverColor(request.coverColor())
                        .build());
        if (completion.isCertified()) {
            throw new BusinessException(ErrorCode.ALREADY_COMPLETED);
        }
        completion.certify(reviewText, completedAt, COMPLETION_REWARD_POINT, LocalDateTime.now(KST));

        // 5~6. 보상 지급 (Dirty Checking 으로 트랜잭션 종료 시 UPDATE)
        ProfileSkinResult skin = extractSkinColors(book);
        user.applyProfileSkin(skin.backgroundColorHex(), skin.borderColorHex());
        user.addRewardPoint(COMPLETION_REWARD_POINT);

        try {
            bookCompletionRepository.saveAndFlush(completion);
        } catch (DataIntegrityViolationException e) {
            // 같은 요청이 동시에 두 번 들어온 경우 — DB unique 제약(uk_completion_user_book)이 최종 방어선
            throw new BusinessException(ErrorCode.ALREADY_COMPLETED);
        }

        log.info("[BookCompletion] 완독 인증 완료 - userId={}, bookId={}, skin={}, point={}",
                userId, book.getId(), skin.backgroundColorHex(), user.getRewardPoint());

        return BookCompletionResponseDto.of(completion, user, reviewText.length());
    }

    // =========================================================================
    //  Private 헬퍼 메서드
    // =========================================================================

    /**
     * 독후감이 최소 글자 수를 충족하는지 서비스 레벨에서 엄격히 검증합니다.
     *
     * <p>컨트롤러의 {@code @Valid + @Size} 가 1차 방어선이지만, 앞뒤 공백만으로 200자를 채우는 경우는
     * 막지 못하므로 여기서 trim 후 다시 셉니다. 서비스가 직접 호출될 때를 대비한 2차 방어선이기도 합니다.
     *
     * <p>글자 수는 {@link String#length()}(UTF-16 단위)로 셉니다. 프론트의 {@code String.length} 와
     * 기준이 같아서 화면의 카운터와 서버 판정이 어긋나지 않습니다.
     *
     * @return 앞뒤 공백을 제거한 독후감
     */
    private String validateReviewLength(String review) {
        String trimmed = review == null ? "" : review.trim();
        if (trimmed.length() < MINIMUM_REVIEW_LENGTH) {
            throw new BusinessException(ErrorCode.REVIEW_TOO_SHORT,
                    String.format("독후감은 최소 %d자 이상 작성해야 완독 인증이 가능해요. (현재 %d자)",
                            MINIMUM_REVIEW_LENGTH, trimmed.length()));
        }
        return trimmed;
    }

    private LocalDate resolveCompletedAt(LocalDate completedAt) {
        LocalDate today = LocalDate.now(KST);
        if (completedAt == null) {
            return today;
        }
        if (completedAt.isAfter(today)) {
            throw new BusinessException(ErrorCode.INVALID_COMPLETION_DATE);
        }
        return completedAt;
    }

    /**
     * 도서 표지 이미지 URL에서 프로필 스킨 색상을 추출합니다.
     *
     * <p>표지 이미지 URL이 없거나, 색상 추출에 실패한 경우 기본 스킨을 반환합니다.
     * (완독 인증 자체가 실패해서는 안 되므로 색상 추출은 Best-Effort 처리)
     */
    private ProfileSkinResult extractSkinColors(Book book) {
        String coverUrl = book.getCoverImageUrl();

        if (!StringUtils.hasText(coverUrl)) {
            log.debug("[BookCompletion] 도서 표지 URL이 없어 기본 스킨을 지급합니다. bookId={}", book.getId());
            return ProfileSkinResult.DEFAULT;
        }

        try {
            String primaryColor = colorExtractorService.extractPrimaryColorHex(coverUrl);
            String borderColor = colorExtractorService.deriveBorderColor(primaryColor);
            return new ProfileSkinResult(primaryColor, borderColor);

        } catch (ColorExtractionException e) {
            log.warn("[BookCompletion] 색상 추출 실패, 기본 스킨 지급. bookId={}, 원인={}", book.getId(), e.getMessage());
            return ProfileSkinResult.DEFAULT;
        }
    }

    /**
     * 지급할 프로필 스킨 색상
     *
     * @param backgroundColorHex 배경색 Hex Code (예: "#1B4D3E")
     * @param borderColorHex     테두리색 Hex Code (예: "#133A2F")
     */
    record ProfileSkinResult(String backgroundColorHex, String borderColorHex) {
        static final ProfileSkinResult DEFAULT = new ProfileSkinResult(DEFAULT_BACKGROUND_COLOR, DEFAULT_BORDER_COLOR);
    }
}
