package com.bookcanvas.domain.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 상점 판매 상품. 코드 규칙: 테두리 장식 중 단색 링은 "BORDER_RRGGBB", 그 밖의 장식은 "GOLD_RING" 등. 모두 User.profileFrame 에 저장된다.
 * 프런트(utils/shopItems.js). 배경색은 무료라 상품이 아니다의 목록과 코드·가격을 맞춘다.
 */
public record ShopItem(String code, int price) {

    private static final String BORDER_PREFIX = "BORDER_";
    private static final int COLOR_PRICE = 50;

    private static final List<String> BORDERS =
            List.of("FFFFFF", "FBBF24", "34D399", "60A5FA", "A78BFA", "F472B6", "F87171", "212529");

    private static final List<ShopItem> CATALOG = buildCatalog();

    private static List<ShopItem> buildCatalog() {
        List<ShopItem> items = new ArrayList<>();
        BORDERS.forEach(c -> items.add(new ShopItem(BORDER_PREFIX + c, COLOR_PRICE)));
        items.add(new ShopItem("DEMIAN", 0)); // 『데미안』 모티브 — 0P 교환
        items.add(new ShopItem("GOLD_RING", 100));
        return List.copyOf(items);
    }

    public static Optional<ShopItem> fromCode(String code) {
        return CATALOG.stream().filter(i -> i.code.equals(code)).findFirst();
    }

}
