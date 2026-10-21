package com.bookcanvas.domain.quote.service;

import com.bookcanvas.domain.book.entity.Book;
import com.bookcanvas.domain.book.service.BookFinder;
import com.bookcanvas.domain.quote.dto.CommentDto;
import com.bookcanvas.domain.quote.dto.QuoteRequestDto;
import com.bookcanvas.domain.quote.dto.QuoteResponse;
import com.bookcanvas.domain.quote.entity.Quote;
import com.bookcanvas.domain.quote.entity.QuoteComment;
import com.bookcanvas.domain.quote.entity.QuoteLike;
import com.bookcanvas.domain.quote.entity.QuoteTagMap;
import com.bookcanvas.domain.quote.repository.QuoteCommentRepository;
import com.bookcanvas.domain.quote.repository.QuoteLikeRepository;
import com.bookcanvas.domain.quote.repository.QuoteRepository;
import com.bookcanvas.domain.tag.entity.Tag;
import com.bookcanvas.domain.tag.repository.TagRepository;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.common.dto.PageResponse;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 커뮤니티 피드: 구절(글) 작성·조회·수정·삭제, 좋아요, 댓글
 *
 * <p><b>피드 조회 시 발행되는 쿼리 (페이지 크기와 무관하게 고정):</b>
 * <pre>
 *   1. 구절 + 작성자 + 도서 (fetch join, LIMIT)   2. 전체 건수
 *   3. 태그 매핑 IN 쿼리 (@BatchSize)            4. 좋아요 수 GROUP BY
 *   5. 댓글 수 GROUP BY                          6. 내가 좋아요한 글 (로그인 시)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuoteRepository quoteRepository;
    private final QuoteLikeRepository quoteLikeRepository;
    private final QuoteCommentRepository quoteCommentRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final BookFinder bookFinder;

    /**
     * 공개 피드 조회
     *
     * @param currentUserId 로그인 사용자 (비로그인이면 null) — liked / mine 계산용
     * @param tagIds        태그 필터 (비어 있으면 전체)
     */
    public PageResponse<QuoteResponse> getFeed(Long currentUserId, List<Long> tagIds, Pageable pageable) {
        Page<Quote> page;
        if (tagIds == null || tagIds.isEmpty()) {
            page = quoteRepository.findPublicFeed(pageable);
        } else {
            validateTagIds(tagIds);
            page = quoteRepository.findByTagIds(tagIds, pageable);
        }

        List<Long> ids = page.getContent().stream().map(Quote::getId).toList();
        Map<Long, Long> likeCounts = ids.isEmpty() ? Map.of() : toCountMap(quoteLikeRepository.countGroupByQuoteIds(ids));
        Map<Long, Long> commentCounts = ids.isEmpty() ? Map.of() : toCountMap(quoteCommentRepository.countGroupByQuoteIds(ids));
        Set<Long> likedIds = (currentUserId == null || ids.isEmpty())
                ? Set.of()
                : new HashSet<>(quoteLikeRepository.findLikedQuoteIds(currentUserId, ids));

        return PageResponse.from(page.map(q -> QuoteResponse.of(
                q,
                likeCounts.getOrDefault(q.getId(), 0L),
                commentCounts.getOrDefault(q.getId(), 0L),
                likedIds.contains(q.getId()),
                currentUserId)));
    }

    @Transactional
    public QuoteResponse create(Long userId, QuoteRequestDto request) {
        User user = getUser(userId);
        Tag tag = getTagByName(request.tagName());
        Book book = bookFinder.findOrCreate(request.bookTitle(), request.author());

        Quote quote = Quote.builder()
                .content(request.content())
                .isPublic(true)
                .user(user)
                .book(book)
                .build();
        QuoteTagMap.of(quote, tag);
        quoteRepository.saveAndFlush(quote);

        log.info("[Quote] 작성 - userId={}, quoteId={}", userId, quote.getId());
        return QuoteResponse.of(quote, 0, 0, false, userId);
    }

    @Transactional
    public QuoteResponse update(Long userId, Long quoteId, QuoteRequestDto request) {
        Quote quote = getOwnedQuote(userId, quoteId);
        Tag tag = getTagByName(request.tagName());
        Book book = bookFinder.findOrCreate(request.bookTitle(), request.author());

        quote.updateContent(request.content(), quote.getMemo(), quote.getPageNumber());
        quote.changeBook(book);
        quote.replaceTag(tag);
        quote.markEdited(LocalDateTime.now(KST));
        quoteRepository.flush();

        return QuoteResponse.of(
                quote,
                quoteLikeRepository.countByQuoteId(quoteId),
                quoteCommentRepository.countByQuoteId(quoteId),
                quoteLikeRepository.existsByUserIdAndQuoteId(userId, quoteId),
                userId);
    }

    @Transactional
    public void delete(Long userId, Long quoteId) {
        Quote quote = getOwnedQuote(userId, quoteId);
        // 좋아요·댓글은 Quote 와 양방향 관계가 없으므로 먼저 지운다 (FK 제약)
        quoteLikeRepository.deleteAllByQuoteId(quoteId);
        quoteCommentRepository.deleteAllByQuoteId(quoteId);
        quoteRepository.delete(quote);
        log.info("[Quote] 삭제 - userId={}, quoteId={}", userId, quoteId);
    }

    // ── 좋아요 (같은 요청을 여러 번 보내도 결과가 같도록 멱등하게) ──

    @Transactional
    public CommentDto.LikeResponse like(Long userId, Long quoteId) {
        Quote quote = getQuote(quoteId);
        if (!quoteLikeRepository.existsByUserIdAndQuoteId(userId, quoteId)) {
            try {
                quoteLikeRepository.saveAndFlush(new QuoteLike(getUser(userId), quote));
            } catch (DataIntegrityViolationException e) {
                // 더블 탭으로 동시에 두 번 들어온 경우 — 이미 좋아요 상태이므로 무시
                log.debug("[Quote] 좋아요 중복 요청 무시 - userId={}, quoteId={}", userId, quoteId);
            }
        }
        return new CommentDto.LikeResponse(quoteId, true, quoteLikeRepository.countByQuoteId(quoteId));
    }

    @Transactional
    public CommentDto.LikeResponse unlike(Long userId, Long quoteId) {
        getQuote(quoteId);
        quoteLikeRepository.deleteByUserIdAndQuoteId(userId, quoteId);
        return new CommentDto.LikeResponse(quoteId, false, quoteLikeRepository.countByQuoteId(quoteId));
    }

    // ── 댓글 ──

    public List<CommentDto.Response> getComments(Long currentUserId, Long quoteId) {
        getQuote(quoteId);
        return quoteCommentRepository.findByQuoteIdWithUser(quoteId).stream()
                .map(c -> CommentDto.Response.of(c, currentUserId))
                .toList();
    }

    @Transactional
    public CommentDto.Response addComment(Long userId, Long quoteId, CommentDto.Request request) {
        QuoteComment comment = new QuoteComment(getUser(userId), getQuote(quoteId), request.content());
        quoteCommentRepository.saveAndFlush(comment);
        return CommentDto.Response.of(comment, userId);
    }

    // =========================================================================

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Quote getQuote(Long quoteId) {
        return quoteRepository.findById(quoteId).orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_NOT_FOUND));
    }

    private Quote getOwnedQuote(Long userId, Long quoteId) {
        Quote quote = getQuote(quoteId);
        if (!quote.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.QUOTE_FORBIDDEN);
        }
        return quote;
    }

    private Tag getTagByName(String name) {
        return tagRepository.findByName(name).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TAG));
    }

    /** 존재하지 않는 태그 ID 가 섞여 있으면 클라이언트 버그를 빨리 알 수 있도록 400 */
    private void validateTagIds(List<Long> tagIds) {
        long existCount = tagRepository.countByIdIn(tagIds);
        if (existCount != tagIds.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.INVALID_TAG);
        }
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }
}
