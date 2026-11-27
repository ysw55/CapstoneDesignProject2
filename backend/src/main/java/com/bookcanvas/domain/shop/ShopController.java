package com.bookcanvas.domain.shop;

import com.bookcanvas.domain.user.dto.UserResponseDto;
import com.bookcanvas.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 상점 API — {@code POST /api/shop/items/{code}/purchase} 상품 구매 → 200 + 갱신된 내 정보 (JWT 필요)
 */
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @PostMapping("/items/{code}/purchase")
    public ResponseEntity<UserResponseDto> purchase(@AuthenticationPrincipal AuthUser authUser, @PathVariable String code) {
        return ResponseEntity.ok(shopService.purchase(authUser.userId(), code));
    }
}
