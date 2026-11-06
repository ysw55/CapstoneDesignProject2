package com.bookcanvas.domain.book.service;

import com.bookcanvas.domain.book.dto.ShelfBookRequestDto;
import com.bookcanvas.domain.book.dto.ShelfBookResponseDto;
import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.entity.BookCompletion;
import com.bookcanvas.domain.book.entity.Bookshelf;
import com.bookcanvas.domain.book.service.BookCoverColorExtractorService.CoverPalette;
import com.bookcanvas.domain.book.repository.BookCompletionRepository;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 내 책장 조회 / 추가 / 수정 / 삭제 (독후감 없는 일반 등록)
 *
 * <p>완독 인증은 {@link BookCompletionService} 가 담당하며, 같은 책장 항목에 인증 정보를 더합니다.
 * 인증된 책은 보상 중복 지급을 막기 위해 표지 색만 바꿀 수 있고 삭제할 수 없습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookShelfService {

    private final BookCompletionRepository bookCompletionRepository;
    private final UserRepository userRepository;
    private final BookFinder bookFinder;
    private final BookshelvesService bookshelvesService;
    private final BookCoverColorExtractorService colorExtractor;

    private static final int BACKFILL_LIMIT = 8;
    private static final long BACKFILL_TIMEOUT_SECONDS = 6;

    /** 로그인 사용자의 책 전체 — 모든 책장과 보관함 (직접 정한 순서, 없으면 최근 완독 순) */
    @Transactional // 책장이 정해지지 않은 책을 첫 번째 책장에 채워 넣기 때문에 쓰기 트랜잭션
    public List<ShelfBookResponseDto> getMyShelf(Long userId) {
        bookshelvesService.ensureDefaultShelf(userId);
        List<BookCompletion> shelf = bookCompletionRepository.findShelfByUserId(userId);
        backfillCoverPalettes(shelf);
        return shelf.stream().map(ShelfBookResponseDto::from).toList();
    }

    @Transactional
    public ShelfBookResponseDto addBook(Long userId, ShelfBookRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String coverUrl = BookSearchService.isTrustedCoverUrl(request.coverImageUrl()) ? request.coverImageUrl().trim() : null;
        String spineUrl = BookSearchService.isTrustedCoverUrl(request.spineImageUrl()) ? request.spineImageUrl().trim() : null;
        String productUrl = BookSearchService.isTrustedProductUrl(request.productUrl()) ? request.productUrl().trim() : null;
        Book book = bookFinder.findOrCreate(request.title(), request.author(), request.isbn(), request.publisher(), coverUrl);
        book.attachYes24Info(spineUrl, productUrl);
        Bookshelf shelf = request.shelfId() == null
                ? bookshelvesService.ensureDefaultShelf(userId)
                : bookshelvesService.findOwned(userId, request.shelfId());

        var existing = bookCompletionRepository.findByUserIdAndBookId(userId, book.getId());
        if (existing.isPresent()) {
            throw new BusinessException(existing.get().isArchived() ? ErrorCode.ALREADY_ARCHIVED : ErrorCode.ALREADY_ON_SHELF);
        }

        // 표지가 있으면 한 번만 내려받아 책등 색(대표 색 + 대표 색 3가지)을 뽑는다
        CoverPalette palette = null;
        // 책등 이미지가 있으면 색 블록은 필요 없으므로 표지를 내려받지 않는다
        if (coverUrl != null && book.getSpineImageUrl() == null
                && (request.coverColor() == null || !book.hasCoverPaletteResolved())) {
            palette = extractPalette(coverUrl);
        }
        if (palette != null && !book.hasCoverPaletteResolved()) {
            book.changeCoverPalette(palette.colors());
        }

        BookCompletion entry = BookCompletion.builder()
                .user(user)
                .book(book)
                .shelf(shelf)
                .completedAt(request.completedAt())
                // 색을 고르지 않았고 표지가 있으면 표지의 대표 색을 책등 가운데 색으로 쓴다
                .coverColor(request.coverColor() != null ? request.coverColor() : (palette != null ? palette.primaryColorHex() : null))
                .build();
        saveAndFlush(entry);

        log.info("[Shelf] 책장 추가 - userId={}, bookId={}", userId, book.getId());
        return ShelfBookResponseDto.from(entry);
    }

    @Transactional
    public ShelfBookResponseDto updateBook(Long userId, Long completionId, ShelfBookRequestDto request) {
        BookCompletion entry = findOwned(userId, completionId);

        if (entry.isCertified()) {
            // 인증한 책: 표지 색만 변경 가능
            boolean infoChanged = !entry.getBook().getTitle().equals(request.title())
                    || !entry.getBook().getAuthor().equals(request.author())
                    || !entry.getCompletedAt().equals(request.completedAt());
            if (infoChanged) {
                throw new BusinessException(ErrorCode.CERTIFIED_BOOK_LOCKED);
            }
        } else {
            Book book = bookFinder.findOrCreate(request.title(), request.author());
            boolean otherEntryExists = !Objects.equals(book.getId(), entry.getBook().getId())
                    && bookCompletionRepository.findByUserIdAndBookId(userId, book.getId()).isPresent();
            if (otherEntryExists) {
                throw new BusinessException(ErrorCode.ALREADY_ON_SHELF);
            }
            entry.changeBook(book, request.completedAt());
        }

        if (request.coverColor() != null) {
            entry.changeCoverColor(request.coverColor());
        }
        saveAndFlush(entry);
        return ShelfBookResponseDto.from(entry);
    }

    /**
     * 책장 순서 저장. 그 책장에 꽂힌(보관하지 않은) 모든 책을 원하는 순서대로 한 번씩 담아 보내야 한다.
     * (일부만 오거나 남의 책·중복이 섞이면 거절 — 화면이 오래된 상태일 수 있음)
     */
    @Transactional
    public void reorder(Long userId, Long shelfId, List<Long> completionIds) {
        bookshelvesService.ensureDefaultShelf(userId);
        // 순서는 그 책장에 꽂힌 책끼리만 정한다 (보관함의 책은 제외, shelfId 가 없으면 모든 책장)
        List<BookCompletion> mine = bookCompletionRepository.findShelfByUserId(userId).stream()
                .filter(c -> !c.isArchived())
                .filter(c -> shelfId == null || (c.getShelf() != null && shelfId.equals(c.getShelf().getId())))
                .toList();
        Map<Long, BookCompletion> byId = mine.stream()
                .collect(Collectors.toMap(BookCompletion::getId, Function.identity()));

        if (completionIds.size() != byId.size() || !byId.keySet().equals(new HashSet<>(completionIds))) {
            throw new BusinessException(ErrorCode.INVALID_SHELF_ORDER);
        }
        for (int i = 0; i < completionIds.size(); i++) {
            byId.get(completionIds.get(i)).moveTo(i);
        }
        log.info("[Shelf] 순서 변경 - userId={}, count={}", userId, completionIds.size());
    }

    /** 책장 → 보관함. 인증한 책도 가능 (삭제와 달리 기록·포인트는 그대로) */
    @Transactional
    public ShelfBookResponseDto archiveBook(Long userId, Long completionId) {
        BookCompletion entry = findOwned(userId, completionId);
        entry.archive();
        log.info("[Shelf] 보관함으로 - userId={}, completionId={}", userId, completionId);
        return ShelfBookResponseDto.from(entry);
    }

    /** 보관함 → 책장. shelfId 가 없으면 원래 있던 책장, 있으면 그 책장의 맨 앞에 꽂힌다 */
    @Transactional
    public ShelfBookResponseDto unarchiveBook(Long userId, Long completionId, Long shelfId) {
        BookCompletion entry = findOwned(userId, completionId);
        Bookshelf target = shelfId == null ? null : bookshelvesService.findOwned(userId, shelfId);
        entry.unarchive();
        if (target != null) entry.moveToShelf(target);
        log.info("[Shelf] 책장으로 - userId={}, completionId={}", userId, completionId);
        return ShelfBookResponseDto.from(entry);
    }

    @Transactional
    public void deleteBook(Long userId, Long completionId) {
        BookCompletion entry = findOwned(userId, completionId);
        if (entry.isCertified()) {
            throw new BusinessException(ErrorCode.CERTIFIED_BOOK_LOCKED);
        }
        bookCompletionRepository.delete(entry);
        log.info("[Shelf] 책장에서 삭제 - userId={}, completionId={}", userId, completionId);
    }

    // =========================================================================

    /** 표지에서 책등 색을 뽑는다. 실패해도 책 추가는 되어야 하므로 null(= 기본 색, 3단 꾸밈 없음)로 넘어간다 */
    private CoverPalette extractPalette(String coverUrl) {
        try {
            return colorExtractor.extractPalette(coverUrl);
        } catch (BookCoverColorExtractorService.ColorExtractionException e) {
            log.warn("[Shelf] 표지 색 추출 실패, 기본 색 사용 - {}", e.getMessage());
            return null;
        }
    }

    /**
     * 표지 대표 색이 아직 없는 책(기능 추가 전에 꽂은 책)을 채운다.
     *
     * <p>표지를 병렬로 내려받고, 한 번에 {@value #BACKFILL_LIMIT}권·{@value #BACKFILL_TIMEOUT_SECONDS}초까지만 기다린다.
     * 시간 안에 끝나지 못한 책은 다음 조회 때 이어서 채우고, 읽을 수 없는 표지는 빈 목록으로 기록해 다시 시도하지 않는다.
     */
    private void backfillCoverPalettes(List<BookCompletion> completions) {
        Map<Long, Book> targets = new LinkedHashMap<>();
        for (BookCompletion c : completions) {
            Book book = c.getBook();
            if (!book.hasCoverPaletteResolved() && book.getSpineImageUrl() == null
                    && BookSearchService.isTrustedCoverUrl(book.getCoverImageUrl())) {
                targets.putIfAbsent(book.getId(), book);
                if (targets.size() >= BACKFILL_LIMIT) break;
            }
        }
        if (targets.isEmpty()) return;

        Map<Long, CompletableFuture<CoverPalette>> futures = new LinkedHashMap<>();
        targets.forEach((id, book) -> futures.put(id,
                CompletableFuture.supplyAsync(() -> colorExtractor.extractPalette(book.getCoverImageUrl()))));

        try {
            CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new))
                    .get(BACKFILL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            // 일부가 실패하거나 늦어도 아래에서 책별로 처리한다
        }

        targets.forEach((id, book) -> {
            CompletableFuture<CoverPalette> future = futures.get(id);
            if (!future.isDone()) return; // 시간 초과 — 다음 조회 때 다시
            CoverPalette palette = future.isCompletedExceptionally() ? null : future.join();
            book.changeCoverPalette(palette == null ? List.of() : palette.colors());
        });
    }

    /** 남의 책장 항목은 '없는 것'으로 응답 (존재 여부도 노출하지 않음) */
    private BookCompletion findOwned(Long userId, Long completionId) {
        return bookCompletionRepository.findByIdAndUserId(completionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHELF_BOOK_NOT_FOUND));
    }

    private void saveAndFlush(BookCompletion entry) {
        try {
            bookCompletionRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException e) {
            // 같은 책을 동시에 두 번 추가한 경우 — DB unique 제약이 최종 방어선
            throw new BusinessException(ErrorCode.ALREADY_ON_SHELF);
        }
    }
}
