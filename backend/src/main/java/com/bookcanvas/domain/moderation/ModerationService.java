package com.bookcanvas.domain.moderation;

import com.bookcanvas.domain.quote.dto.QuoteResponse;
import com.bookcanvas.domain.quote.entity.Quote;
import com.bookcanvas.domain.quote.repository.QuoteRepository;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 커뮤니티 글 신고와 사용자 차단 (같은 요청을 여러 번 보내도 결과가 같다) */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModerationService {

    private final QuoteReportRepository quoteReportRepository;
    private final UserBlockRepository userBlockRepository;
    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;

    @Transactional
    public void report(Long userId, Long quoteId, ReportReason reason) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_NOT_FOUND));
        if (quote.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_OWN_QUOTE);
        }
        if (quoteReportRepository.existsByReporterIdAndQuoteId(userId, quoteId)) return;
        quoteReportRepository.save(new QuoteReport(getUser(userId), quote, reason));
        log.info("[Report] 신고 - userId={}, quoteId={}, reason={}", userId, quoteId, reason);
    }

    @Transactional
    public void block(Long userId, Long targetId) {
        if (userId.equals(targetId)) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        User target = getUser(targetId);
        if (userBlockRepository.existsByBlockerIdAndBlockedId(userId, targetId)) return;
        userBlockRepository.save(new UserBlock(getUser(userId), target));
    }

    @Transactional
    public void unblock(Long userId, Long targetId) {
        userBlockRepository.deleteByBlockerIdAndBlockedId(userId, targetId);
    }

    public List<QuoteResponse.AuthorInfo> getBlockedUsers(Long userId) {
        return userBlockRepository.findByBlockerId(userId).stream()
                .map(b -> QuoteResponse.AuthorInfo.from(b.getBlocked()))
                .toList();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
