package com.bookcanvas.domain.shop;

import com.bookcanvas.domain.book.repository.BookCompletionRepository;
import com.bookcanvas.domain.user.dto.UserResponseDto;
import com.bookcanvas.domain.user.entity.User;
import com.bookcanvas.domain.user.repository.UserRepository;
import com.bookcanvas.global.error.BusinessException;
import com.bookcanvas.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 포인트로 상점 상품 구매 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final UserRepository userRepository;
    private final BookCompletionRepository bookCompletionRepository;

    /**
     * @throws BusinessException SHOP_ITEM_NOT_FOUND, ALREADY_OWNED_ITEM, NOT_ENOUGH_POINT
     */
    @Transactional
    public UserResponseDto purchase(Long userId, String code) {
        ShopItem item = ShopItem.fromCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_ITEM_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.owns(item.code())) {
            throw new BusinessException(ErrorCode.ALREADY_OWNED_ITEM);
        }
        if (user.getRewardPoint() < item.price()) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_POINT);
        }
        user.spendRewardPoint(item.price());
        user.addOwnedItem(item.code());
        return UserResponseDto.of(user, bookCompletionRepository.countByUserIdAndCertifiedAtIsNotNull(userId));
    }
}
