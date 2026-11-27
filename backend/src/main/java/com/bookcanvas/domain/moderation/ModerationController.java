package com.bookcanvas.domain.moderation;

import com.bookcanvas.domain.quote.dto.QuoteResponse;
import com.bookcanvas.global.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 신고 · 차단 API (모두 JWT 필요)
 * <ul>
 *   <li>{@code POST   /api/quotes/{id}/report}  글 신고 → 204</li>
 *   <li>{@code POST   /api/users/{id}/block}    사용자 차단 → 204</li>
 *   <li>{@code DELETE /api/users/{id}/block}    차단 해제 → 204</li>
 *   <li>{@code GET    /api/users/me/blocks}     내가 차단한 사용자 목록</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    public record ReportRequest(@NotNull(message = "신고 사유를 선택해 주세요.") ReportReason reason) {
    }

    @PostMapping("/quotes/{quoteId}/report")
    public ResponseEntity<Void> report(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long quoteId,
            @Valid @RequestBody ReportRequest request
    ) {
        moderationService.report(authUser.userId(), quoteId, request.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/block")
    public ResponseEntity<Void> block(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long userId) {
        moderationService.block(authUser.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}/block")
    public ResponseEntity<Void> unblock(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long userId) {
        moderationService.unblock(authUser.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/blocks")
    public ResponseEntity<List<QuoteResponse.AuthorInfo>> blockedUsers(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(moderationService.getBlockedUsers(authUser.userId()));
    }
}
