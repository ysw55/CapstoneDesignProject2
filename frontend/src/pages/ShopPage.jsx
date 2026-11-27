import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { Check, ChevronLeft, Coins } from 'lucide-react';
import ProfileAvatar from '../components/common/ProfileAvatar';
import { SHOP_SECTIONS } from '../utils/shopItems';
import { useAuth } from '../context/AuthContext';

/** 프로필 > 상점 (/profile/shop) — 로그인 필수(RequireAuth). 상품 목록은 utils/shopItems.js */
export default function ShopPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, purchaseItem } = useAuth();
  const [buying, setBuying] = useState(null);
  const [error, setError] = useState('');

  const handleBack = () => (location.key === 'default' ? navigate('/profile', { replace: true }) : navigate(-1));

  const handleBuy = async (item) => {
    if (!window.confirm(item.price === 0 ? `${item.name}을(를) 0P로 교환할까요?` : `${item.name}을(를) ${item.price}P로 구매할까요?`)) return;
    setBuying(item.code);
    setError('');
    try {
      await purchaseItem(item.code);
    } catch (e) {
      setError(e?.message || '구매하지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      setBuying(null);
    }
  };

  return (
    <>
      <TopBar>
        <IconBtn type="button" aria-label="뒤로 가기" onClick={handleBack}>
          <ChevronLeft size={26} strokeWidth={2} />
        </IconBtn>
        <TopTitle>상점</TopTitle>
        <span aria-hidden style={{ width: 44 }} />
      </TopBar>

      <Balance>
        <Coins size={18} strokeWidth={2} aria-hidden />
        <span>보유 포인트</span>
        <strong>{(user?.rewardPoint ?? 0).toLocaleString('ko-KR')}P</strong>
      </Balance>

      {SHOP_SECTIONS.map((section) => (
        <Section key={section.title}>
          <h2>{section.title}</h2>
          <List>
            {section.items.map((item) => {
              const owned = user?.ownedItems?.includes(item.code);
              const canAfford = (user?.rewardPoint ?? 0) >= item.price;
              return (
                <Item key={item.code}>
                  <ProfileAvatar
                    name={user?.username}
                    background={user?.profileBackgroundColor}
                    image={user?.profileImage}
                    frame={item.code}
                    size={48}
                  />
                  <ItemText>
                    <strong>{item.name}</strong>
                    {item.description && <span>{item.description}</span>}
                  </ItemText>
                  {owned ? (
                    <Owned type="button" onClick={() => navigate('/profile/edit')}>
                      <Check size={14} strokeWidth={3} aria-hidden /> 적용하기
                    </Owned>
                  ) : (
                    <Buy type="button" disabled={!canAfford || buying === item.code} onClick={() => handleBuy(item)}>
                      {item.price === 0 ? '0P 교환' : `${item.price}P`}
                    </Buy>
                  )}
                </Item>
              );
            })}
          </List>
        </Section>
      ))}
      {error && <ErrorText role="alert">{error}</ErrorText>}
    </>
  );
}

const TopBar = styled.header`
  position: sticky;
  top: env(safe-area-inset-top, 0px);
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 ${({ theme }) => theme.space(2)};
  background: ${({ theme }) => theme.colors.background};
`;

const IconBtn = styled.button`
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;

  &:active {
    background: ${({ theme }) => theme.colors.surfaceMuted};
  }
`;

const TopTitle = styled.h1`
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  letter-spacing: -0.03em;
`;

const Balance = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(2)};
  margin: ${({ theme }) => theme.space(2)} ${({ theme }) => theme.layout.gutter} 0;
  padding: ${({ theme }) => `${theme.space(4)} ${theme.space(4)}`};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceMuted};
  color: ${({ theme }) => theme.colors.textSecondary};
  font-size: ${({ theme }) => theme.fontSize.sm};

  strong {
    margin-left: auto;
    color: ${({ theme }) => theme.colors.textPrimary};
    font-size: ${({ theme }) => theme.fontSize.lg};
  }
`;

const Section = styled.section`
  margin: ${({ theme }) => theme.space(5)} ${({ theme }) => theme.layout.gutter} 0;

  h2 {
    margin-bottom: ${({ theme }) => theme.space(2)};
    font-size: 14px;
    font-weight: 700;
  }
`;

const List = styled.ul`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(3)};
`;

const Item = styled.li`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(4)};
  padding: ${({ theme }) => theme.space(4)};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  background: ${({ theme }) => theme.colors.surface};
`;

const ItemText = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;

  strong {
    font-size: 15px;
    font-weight: 700;
  }

  span {
    font-size: 12.5px;
    color: ${({ theme }) => theme.colors.textSecondary};
  }
`;

const pill = `
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
`;

const Buy = styled.button`
  ${pill}
  color: ${({ theme }) => theme.colors.onAccent};
  background: ${({ theme }) => theme.colors.accent};

  &:disabled {
    opacity: 0.4;
  }
`;

const Owned = styled.button`
  ${pill}
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.accentSoft};
`;

const ErrorText = styled.p`
  margin: ${({ theme }) => theme.space(3)} ${({ theme }) => theme.layout.gutter} 0;
  font-size: 13px;
  color: #e03131;
`;
