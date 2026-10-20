import React from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { BadgeCheck, ChevronRight, Coins, LogIn, Settings } from 'lucide-react';
import ScreenHeader from '../components/common/ScreenHeader';
import { IconButton } from '../components/common/IconButton';
import Button from '../components/common/Button';
import ProfileAvatar from '../components/common/ProfileAvatar';
import SocialLoginButtons from '../components/auth/SocialLoginButtons';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const navigate = useNavigate();
  const { user, status, isLoggedIn, openLogin } = useAuth();

  return (
    <>
      <ScreenHeader
        title="프로필"
        right={
          <IconButton aria-label="설정" onClick={() => navigate('/profile/settings')}>
            <Settings size={22} strokeWidth={1.8} />
          </IconButton>
        }
      />

      <Content>
        {status === 'restoring' ? (
          <Card aria-busy="true">
            <Muted>로그인 정보를 확인하고 있어요…</Muted>
          </Card>
        ) : isLoggedIn ? (
          <>
            {/* 완독 보상 스킨이 적용된 프로필 카드 */}
            <SkinCard $bg={user.profileBackgroundColor} $border={user.profileBorderColor}>
              <ProfileAvatar
                name={user.username}
                background={user.profileBackgroundColor}
                border={user.profileBorderColor}
                size={64}
              />
              <Name>{user.username}</Name>
              <Muted>{user.email}</Muted>
              {!user.profileBackgroundColor && <SkinHint>첫 완독 인증을 하면 프로필 스킨이 생겨요</SkinHint>}
            </SkinCard>

            <Stats>
              <Stat>
                <Coins size={18} strokeWidth={2} aria-hidden />
                <StatValue>{user.rewardPoint.toLocaleString('ko-KR')}P</StatValue>
                <StatLabel>보상 포인트</StatLabel>
              </Stat>
              <Stat>
                <BadgeCheck size={18} strokeWidth={2} aria-hidden />
                <StatValue>{user.completedBookCount}권</StatValue>
                <StatLabel>완독 인증</StatLabel>
              </Stat>
            </Stats>

            <LinkRow type="button" onClick={() => navigate('/books/certify')}>
              <span>
                <strong>완독 인증하기</strong>
                독후감 200자로 +100P와 새 스킨 받기
              </span>
              <ChevronRight size={20} strokeWidth={2} aria-hidden />
            </LinkRow>
          </>
        ) : (
          <Card>
            <ProfileAvatar name="?" size={64} />
            <Name>로그인이 필요해요</Name>
            <Muted>완독 인증 보상과 프로필 스킨은 계정에 저장돼요</Muted>
            <Button type="button" $variant="primary" $block onClick={() => openLogin()}>
              <LogIn size={18} strokeWidth={2.2} aria-hidden />
              로그인 / 회원가입
            </Button>
            <SocialWrap>
              <SocialLoginButtons compact />
            </SocialWrap>
          </Card>
        )}
      </Content>
    </>
  );
}

const Content = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(3)};
  padding: 0 ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(8)};
`;

const Card = styled.section`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${({ theme }) => theme.space(2)};
  padding: ${({ theme }) => `${theme.space(8)} ${theme.space(5)} ${theme.space(5)}`};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  text-align: center;
  background: ${({ theme }) => theme.colors.surface};

  > button {
    margin-top: ${({ theme }) => theme.space(4)};
  }
`;

/* 보상 스킨 색을 은은하게 깔아 주는 카드 */
const SkinCard = styled(Card)`
  border-color: ${({ $border, theme }) => $border ?? theme.colors.border};
  background:
    linear-gradient(180deg, ${({ $bg }) => ($bg ? `${$bg}1F` : 'transparent')} 0%, transparent 70%),
    ${({ theme }) => theme.colors.surface};
`;

const SocialWrap = styled.div`
  width: 100%;
  margin-top: ${({ theme }) => theme.space(1)};
`;

const Name = styled.h2`
  margin-top: ${({ theme }) => theme.space(2)};
  font-size: ${({ theme }) => theme.fontSize.xl};
  font-weight: 800;
  letter-spacing: -0.03em;
`;

const Muted = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.colors.textSecondary};
`;

const SkinHint = styled.p`
  margin-top: ${({ theme }) => theme.space(2)};
  padding: 4px 10px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.accentSoft};
`;

const Stats = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${({ theme }) => theme.space(3)};
`;

const Stat = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: ${({ theme }) => theme.space(4)};
  border-radius: ${({ theme }) => theme.radius.lg};
  background: ${({ theme }) => theme.colors.backgroundAlt};

  svg {
    margin-bottom: 4px;
    color: ${({ theme }) => theme.colors.accent};
  }
`;

const StatValue = styled.span`
  font-size: ${({ theme }) => theme.fontSize.xl};
  font-weight: 800;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
`;

const StatLabel = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textSecondary};
`;

const LinkRow = styled.button`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  width: 100%;
  padding: ${({ theme }) => theme.space(4)};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  text-align: left;
  background: ${({ theme }) => theme.colors.surface};
  transition: transform ${({ theme }) => theme.motion.fast};

  span {
    display: flex;
    flex-direction: column;
    flex: 1;
    font-size: 12.5px;
    color: ${({ theme }) => theme.colors.textSecondary};
  }

  strong {
    font-size: 15px;
    font-weight: 700;
    color: ${({ theme }) => theme.colors.textPrimary};
  }

  > svg {
    color: ${({ theme }) => theme.colors.textTertiary};
  }

  &:active {
    transform: scale(0.98);
  }
`;
