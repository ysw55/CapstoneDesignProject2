package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.dto.BookCompletionRequestDto;
import com.bookcanvas.domain.book.dto.BookCompletionResponseDto;
import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.entity.BookCompletion;
import com.bookcanvas.domain.book.repository.BookCompletionRepository;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * BookCompletionService 단위 테스트
 *
 * <p>외부 의존성(DB, 실제 이미지 URL)을 Mockito로 격리하여 비즈니스 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class BookCompletionServiceTest {

    @Mock UserRepository userRepository;
    @Mock BookFinder bookFinder;
    @Mock BookCompletionRepository bookCompletionRepository;
    @Mock BookCoverColorExtractorService colorExtractorService;

    @InjectMocks
    BookCompletionService bookCompletionService;

    private static final Long USER_ID = 1L;
    private static final String VALID_REVIEW = "가".repeat(200);
    private static final String SHORT_REVIEW = "짧은 독후감";

    private User mockUser;
    private Book mockBook;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded_pw")
                .build();

        mockBook = Book.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .coverImageUrl("https://example.com/clean-code-cover.jpg")
                .build();
        ReflectionTestUtils.setField(mockBook, "id", 10L);
    }

    private BookCompletionRequestDto request(LocalDate completedAt, String review) {
        return new BookCompletionRequestDto("Clean Code", "Robert C. Martin", null, null, null, completedAt, review, null);
    }

    /** 사용자 조회 + 도서 조회 + (책장에 없음) + 저장 성공까지 준비 */
    private void givenUserAndBook(Book book, Optional<BookCompletion> existing) {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
        given(bookFinder.findOrCreate(eq("Clean Code"), eq("Robert C. Martin"), any(), any(), any())).willReturn(book);
        given(bookCompletionRepository.findByUserIdAndBookId(USER_ID, book.getId())).willReturn(existing);
    }

    private void givenSaveSucceeds() {
        given(bookCompletionRepository.saveAndFlush(any(BookCompletion.class))).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("완독 인증 성공 시, 표지 색상 스킨과 100 포인트가 지급되고 인증된 상태로 책장에 추가된다")
    void certifyCompletion_success() {
        givenUserAndBook(mockBook, Optional.empty());
        givenSaveSucceeds();
        given(colorExtractorService.extractPrimaryColorHex(anyString())).willReturn("#A95BCD");
        given(colorExtractorService.deriveBorderColor("#A95BCD")).willReturn("#6B1E9F");

        BookCompletionResponseDto result =
                bookCompletionService.certifyCompletion(USER_ID, request(LocalDate.of(2026, 10, 1), VALID_REVIEW));

        assertThat(result.reward().profileBackgroundColor()).isEqualTo("#A95BCD");
        assertThat(result.reward().profileBorderColor()).isEqualTo("#6B1E9F");
        assertThat(result.reward().earnedPoint()).isEqualTo(BookCompletionService.COMPLETION_REWARD_POINT);
        assertThat(mockUser.getRewardPoint()).isEqualTo(100);

        ArgumentCaptor<BookCompletion> captor = ArgumentCaptor.forClass(BookCompletion.class);
        then(bookCompletionRepository).should().saveAndFlush(captor.capture());
        BookCompletion saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(mockUser);
        assertThat(saved.getBook()).isSameAs(mockBook);
        assertThat(saved.isCertified()).isTrue();
        assertThat(saved.getCompletedAt()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(saved.getReviewText()).hasSize(200);
    }

    @Test
    @DisplayName("책장에 추가만 해 둔 책을 인증하면, 새로 만들지 않고 그 항목이 인증 상태로 바뀐다")
    void certifyCompletion_upgradesExistingShelfEntry() {
        BookCompletion shelfEntry = BookCompletion.builder()
                .user(mockUser).book(mockBook).completedAt(LocalDate.of(2026, 9, 1)).coverColor("#212529").build();
        givenUserAndBook(mockBook, Optional.of(shelfEntry));
        givenSaveSucceeds();
        given(colorExtractorService.extractPrimaryColorHex(anyString())).willReturn("#112233");
        given(colorExtractorService.deriveBorderColor("#112233")).willReturn("#000000");

        bookCompletionService.certifyCompletion(USER_ID, request(LocalDate.of(2026, 9, 3), VALID_REVIEW));

        then(bookCompletionRepository).should().saveAndFlush(shelfEntry);
        assertThat(shelfEntry.isCertified()).isTrue();
        assertThat(shelfEntry.getCompletedAt()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(shelfEntry.getCoverColor()).isEqualTo("#212529"); // 사용자가 고른 표지 색 유지
    }

    @Test
    @DisplayName("독후감이 200자 미만이면 REVIEW_TOO_SHORT 예외가 발생하고, DB 는 조회하지 않는다")
    void certifyCompletion_fail_shortReview() {
        assertThatThrownBy(() -> bookCompletionService.certifyCompletion(USER_ID, request(null, SHORT_REVIEW)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("200자 이상")
                .hasMessageContaining("현재 6자")
                .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_TOO_SHORT);

        then(userRepository).shouldHaveNoInteractions();
        then(bookCompletionRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("앞뒤 공백으로 200자를 채운 독후감은 통과하지 못한다 (trim 후 199자)")
    void certifyCompletion_fail_paddedWithWhitespace() {
        String padded = "   " + "가".repeat(199) + "   ";
        assertThat(padded.length()).isGreaterThanOrEqualTo(200); // DTO @Size 는 통과하는 길이

        assertThatThrownBy(() -> bookCompletionService.certifyCompletion(USER_ID, request(null, padded)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("현재 199자");
    }

    @Test
    @DisplayName("정확히 200자(경계값)면 인증에 성공한다")
    void certifyCompletion_success_exactly200() {
        givenUserAndBook(mockBook, Optional.empty());
        givenSaveSucceeds();
        given(colorExtractorService.extractPrimaryColorHex(anyString())).willReturn("#112233");
        given(colorExtractorService.deriveBorderColor("#112233")).willReturn("#000000");

        BookCompletionResponseDto result = bookCompletionService.certifyCompletion(USER_ID, request(null, "a".repeat(200)));

        assertThat(result.reviewLength()).isEqualTo(200);
    }

    @Test
    @DisplayName("완독 날짜가 미래면 INVALID_COMPLETION_DATE 예외가 발생한다")
    void certifyCompletion_fail_futureDate() {
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);

        assertThatThrownBy(() -> bookCompletionService.certifyCompletion(USER_ID, request(tomorrow, VALID_REVIEW)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_COMPLETION_DATE);
    }

    @Test
    @DisplayName("완독 날짜를 비우면 오늘 날짜로 기록된다")
    void certifyCompletion_defaultsToToday() {
        givenUserAndBook(mockBook, Optional.empty());
        givenSaveSucceeds();
        given(colorExtractorService.extractPrimaryColorHex(anyString())).willReturn("#112233");
        given(colorExtractorService.deriveBorderColor("#112233")).willReturn("#000000");

        BookCompletionResponseDto result = bookCompletionService.certifyCompletion(USER_ID, request(null, VALID_REVIEW));

        assertThat(result.completedAt()).isEqualTo(LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    @Test
    @DisplayName("이미 인증한 책이면 ALREADY_COMPLETED 예외가 발생하고, 보상은 지급되지 않는다")
    void certifyCompletion_fail_alreadyCompleted() {
        BookCompletion certified = BookCompletion.builder()
                .user(mockUser).book(mockBook).completedAt(LocalDate.of(2026, 9, 1)).build();
        certified.certify(VALID_REVIEW, LocalDate.of(2026, 9, 1), 100, LocalDateTime.now());
        givenUserAndBook(mockBook, Optional.of(certified));

        assertThatThrownBy(() -> bookCompletionService.certifyCompletion(USER_ID, request(null, VALID_REVIEW)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_COMPLETED);

        assertThat(mockUser.getRewardPoint()).isZero();
        then(bookCompletionRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("표지 URL 이 없으면 색상 추출 없이 기본 스킨(딥그린)이 지급된다")
    void certifyCompletion_defaultSkin_noCover() {
        Book noCover = Book.builder().title("Clean Code").author("Robert C. Martin").build();
        ReflectionTestUtils.setField(noCover, "id", 99L);
        givenUserAndBook(noCover, Optional.empty());
        givenSaveSucceeds();

        BookCompletionResponseDto result = bookCompletionService.certifyCompletion(USER_ID, request(null, VALID_REVIEW));

        assertThat(result.reward().profileBackgroundColor()).isEqualTo(BookCompletionService.DEFAULT_BACKGROUND_COLOR);
        then(colorExtractorService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("색상 추출 중 예외가 발생해도 완독 인증은 성공하고 기본 스킨이 지급된다 (Best-Effort)")
    void certifyCompletion_fallback_colorExtractionFails() {
        givenUserAndBook(mockBook, Optional.empty());
        givenSaveSucceeds();
        given(colorExtractorService.extractPrimaryColorHex(anyString()))
                .willThrow(new BookCoverColorExtractorService.ColorExtractionException("네트워크 오류"));

        BookCompletionResponseDto result = bookCompletionService.certifyCompletion(USER_ID, request(null, VALID_REVIEW));

        assertThat(result.reward().profileBackgroundColor()).isEqualTo(BookCompletionService.DEFAULT_BACKGROUND_COLOR);
        assertThat(result.reward().profileBorderColor()).isEqualTo(BookCompletionService.DEFAULT_BORDER_COLOR);
        assertThat(mockUser.getRewardPoint()).isEqualTo(100);
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID로 요청 시 USER_NOT_FOUND 예외가 발생한다")
    void certifyCompletion_fail_userNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookCompletionService.certifyCompletion(999L, request(null, VALID_REVIEW)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
