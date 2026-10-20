import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import {
  Bell,
  ChevronLeft,
  ChevronRight,
  EyeOff,
  KeyRound,
  LogOut,
  Mail,
  Megaphone,
  Moon,
  Coins,
} from 'lucide-react';
import Switch from '../common/Switch';
import { ConfirmDialog } from '../common/Modal';
import { useThemeMode } from '../../context/ThemeModeContext';
import { useAuth } from '../../context/AuthContext';
import ChangePasswordSheet from '../auth/ChangePasswordSheet';
import SocialLoginButtons from '../auth/SocialLoginButtons';
import ProfileAvatar from '../common/ProfileAvatar';

const PROVIDER_LABEL = { LOCAL: '이메일', KAKAO: '카카오', GOOGLE: 'Google' };

export default function Settings() {
  const navigate = useNavigate();
  const location = useLocation();
  const { mode, toggleMode } = useThemeMode();
  const { user, status, isLoggedIn, isSocialAccount, logout, sessionExpired, openLogin } = useAuth();

  const [isPrivate, setIsPrivate] = useState(false);
  const [communityNoti, setCommunityNoti] = useState(true);
  const [serviceNoti, setServiceNoti] = useState(true);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [notice, setNotice] = useState(null);

  const handleBack = () => {
    // 주소로 바로 들어온 경우(뒤로 갈 기록 없음)엔 프로필로 이동
    if (location.key === 'default') navigate('/profile', { replace: true });
    else navigate(-1);
  };


  return (
    <Page>
      <TopBar>
        <BackButton type="button" aria-label="뒤로 가기" onClick={handleBack}>
          <ChevronLeft size={26} strokeWidth={2} />
        </BackButton>
        <TopBarTitle>설정</TopBarTitle>
        <span aria-hidden style={{ width: 44 }} />
      </TopBar>

      {/* 1. 계정 */}
      <Section>
        <SectionTitle>계정</SectionTitle>

        {status === 'restoring' ? (
          <LoginCard aria-busy="true">
            <LoginSub>로그인 정보를 확인하고 있어요…</LoginSub>
          </LoginCard>
        ) : isLoggedIn ? (
          <Group>
            <AccountRow>
              <ProfileAvatar
                name={user.username}
                background={user.profileBackgroundColor}
                border={user.profileBorderColor}
              />
              <RowText>
                <AccountName>{user.username}</AccountName>
                <RowDescription>
                  {PROVIDER_LABEL[user.provider] ?? '이메일'} 계정 · {user.email}
                </RowDescription>
              </RowText>
              <PointBadge>
                <Coins size={14} strokeWidth={2.2} aria-hidden />
                {user.rewardPoint.toLocaleString('ko-KR')}P
              </PointBadge>
            </AccountRow>
            <Row
              as="button"
              type="button"
              disabled={isSocialAccount}
              $disabled={isSocialAccount}
              onClick={() => setPasswordOpen(true)}
            >
              <RowIcon><KeyRound size={18} /></RowIcon>
              <RowText>
                <RowLabel>비밀번호 변경</RowLabel>
                {isSocialAccount && (
                  <RowDescription>{PROVIDER_LABEL[user.provider]} 계정은 비밀번호가 없어요</RowDescription>
                )}
              </RowText>
              {!isSocialAccount && <Chevron />}
            </Row>
            <SwitchRow
              icon={EyeOff}
              label="계정 비공개"
              description={isPrivate ? '내 서재와 글이 나에게만 보여요' : '모든 사용자가 내 서재와 글을 볼 수 있어요'}
              checked={isPrivate}
              onChange={setIsPrivate}
            />
            <Row as="button" type="button" onClick={() => setLogoutOpen(true)}>
              <RowIcon $tone="danger"><LogOut size={18} /></RowIcon>
              <RowText><RowLabel $tone="danger">로그아웃</RowLabel></RowText>
            </Row>
          </Group>
        ) : (
          <LoginCard>
            <LoginHeadline>로그인하고 나만의 서재를 만들어 보세요</LoginHeadline>
            <LoginSub>완독을 인증하면 보상 스킨과 포인트를 받을 수 있어요</LoginSub>

            <SocialButton type="button" $brand="primary" onClick={() => openLogin('login')}>
              <Mail size={18} strokeWidth={2} aria-hidden />
              이메일로 로그인
            </SocialButton>
            <SocialButton type="button" $brand="email" onClick={() => openLogin('signup')}>
              처음이에요 · 회원가입
            </SocialButton>

            <Divider>간편 로그인</Divider>
            <SocialLoginButtons compact />
          </LoginCard>
        )}
        {sessionExpired && !isLoggedIn && (
          <SectionFooter role="status">로그인이 만료됐어요. 다시 로그인해 주세요.</SectionFooter>
        )}
        {notice && isLoggedIn && <SectionFooter role="status" $accent>{notice}</SectionFooter>}
      </Section>

      {/* 2. 화면 및 테마 */}
      <Section>
        <SectionTitle>화면 및 테마</SectionTitle>
        <Group>
          <SwitchRow icon={Moon} label="다크 모드" checked={mode === 'dark'} onChange={toggleMode} />
        </Group>
      </Section>

      {/* 3. 알림 */}
      <Section>
        <SectionTitle>알림</SectionTitle>
        <Group>
          <SwitchRow
            icon={Bell}
            label="커뮤니티 알림"
            description="내 문장에 좋아요나 댓글이 달렸을 때"
            checked={communityNoti}
            onChange={setCommunityNoti}
            disabled={!isLoggedIn}
          />
          <SwitchRow
            icon={Megaphone}
            label="서비스 알림"
            description="새 완독 스킨 보상, 공지사항"
            checked={serviceNoti}
            onChange={setServiceNoti}
            disabled={!isLoggedIn}
          />
        </Group>
        {!isLoggedIn && <SectionFooter>로그인하면 알림을 설정할 수 있어요.</SectionFooter>}
      </Section>

      <Version>Book Canvas v0.1.0</Version>

      <ChangePasswordSheet
        open={passwordOpen}
        onClose={() => setPasswordOpen(false)}
        onChanged={() => setNotice('비밀번호를 변경했어요. 다음 로그인부터 새 비밀번호를 사용해 주세요.')}
      />

      <ConfirmDialog
        open={logoutOpen}
        title="로그아웃할까요?"
        message="이 기기에 저장된 로그인 정보가 삭제돼요."
        confirmLabel="로그아웃"
        onConfirm={() => {
          logout();
          setLogoutOpen(false);
          setNotice(null);
        }}
        onCancel={() => setLogoutOpen(false)}
      />

    </Page>
  );
}

/** 아이콘 + 라벨 + 설명 + 스위치 한 줄. 행 어디를 눌러도 토글된다. */
function SwitchRow({ icon: Icon, label, description, checked, onChange, disabled }) {
  return (
    <Row $disabled={disabled} onClick={() => !disabled && onChange(!checked)}>
      <RowIcon><Icon size={18} /></RowIcon>
      <RowText>
        <RowLabel>{label}</RowLabel>
        {description && <RowDescription>{description}</RowDescription>}
      </RowText>
      <Switch checked={checked} onChange={onChange} label={label} disabled={disabled} />
    </Row>
  );
}

function Chevron() {
  return <ChevronIcon size={20} strokeWidth={2} aria-hidden />;
}

/* ───────────── styles ───────────── */

/* 옅은 회색 바탕 위에 흰 카드 그룹을 올리는 구성 */
const Page = styled.div`
  min-height: 100vh;
  min-height: 100dvh;
  padding-bottom: ${({ theme }) => theme.space(10)};
  background: ${({ theme }) => theme.colors.backgroundAlt};
`;

const TopBar = styled.header`
  position: sticky;
  top: env(safe-area-inset-top, 0px);
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 ${({ theme }) => theme.space(2)};
  background: ${({ theme }) => theme.colors.backgroundAlt};
`;

const BackButton = styled.button`
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  transition: background ${({ theme }) => theme.motion.fast};

  &:active {
    background: ${({ theme }) => theme.colors.surfaceMuted};
  }
`;

const TopBarTitle = styled.h1`
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  letter-spacing: -0.03em;
`;

const Section = styled.section`
  padding: 0 ${({ theme }) => theme.layout.gutter};
  margin-top: ${({ theme }) => theme.space(6)};
`;

const SectionTitle = styled.h2`
  margin: 0 ${({ theme }) => theme.space(1)} ${({ theme }) => theme.space(2)};
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 600;
  color: ${({ theme }) => theme.colors.textSecondary};
`;

const SectionFooter = styled.p`
  margin: ${({ theme }) => theme.space(2)} ${({ theme }) => theme.space(1)} 0;
  font-size: 12px;
  color: ${({ $accent, theme }) => ($accent ? theme.colors.accent : theme.colors.textTertiary)};
`;

const Group = styled.div`
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  background: ${({ theme }) => theme.colors.surface};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const Row = styled.div`
  position: relative;
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  width: 100%;
  min-height: 60px;
  padding: ${({ theme }) => `${theme.space(3)} ${theme.space(4)}`};
  text-align: left;
  cursor: ${({ $disabled }) => ($disabled ? 'default' : 'pointer')};
  user-select: none;
  transition: background ${({ theme }) => theme.motion.fast};

  &:active {
    background: ${({ $disabled, theme }) => ($disabled ? 'transparent' : theme.colors.surfaceMuted)};
  }

  /* 아이콘 오른쪽부터 시작하는 iOS식 구분선 */
  & + &::before {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    left: 60px;
    height: 1px;
    background: ${({ theme }) => theme.colors.border};
  }

  > *:not(:last-child) {
    opacity: ${({ $disabled }) => ($disabled ? 0.45 : 1)};
  }
`;

const AccountRow = styled(Row)`
  min-height: 76px;
  cursor: default;

  &:active {
    background: transparent;
  }
`;

const RowIcon = styled.span`
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  color: ${({ $tone, theme }) => ($tone === 'danger' ? theme.colors.danger : theme.colors.accent)};
  background: ${({ $tone, theme }) => ($tone === 'danger' ? theme.colors.dangerSoft : theme.colors.accentSoft)};
`;

const RowText = styled.span`
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
`;

const RowLabel = styled.span`
  font-size: ${({ theme }) => theme.fontSize.md};
  font-weight: 600;
  letter-spacing: -0.02em;
  color: ${({ $tone, theme }) => ($tone === 'danger' ? theme.colors.danger : theme.colors.textPrimary)};
`;

const RowDescription = styled.span`
  margin-top: 2px;
  font-size: 12.5px;
  line-height: 1.4;
  color: ${({ theme }) => theme.colors.textSecondary};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const ChevronIcon = styled(ChevronRight)`
  flex-shrink: 0;
  color: ${({ theme }) => theme.colors.textTertiary};
`;

const AccountName = styled.span`
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  letter-spacing: -0.03em;
`;

const LoginCard = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(2)};
  padding: ${({ theme }) => `${theme.space(6)} ${theme.space(5)} ${theme.space(5)}`};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  background: ${({ theme }) => theme.colors.surface};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const LoginHeadline = styled.p`
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  letter-spacing: -0.03em;
`;

const LoginSub = styled.p`
  margin-bottom: ${({ theme }) => theme.space(3)};
  font-size: ${({ theme }) => theme.fontSize.sm};
  color: ${({ theme }) => theme.colors.textSecondary};
`;

const SocialButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: ${({ theme }) => theme.space(2)};
  height: 52px;
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: ${({ theme }) => theme.fontSize.md};
  font-weight: 600;
  letter-spacing: -0.02em;
  color: ${({ $brand, theme }) => ($brand === 'primary' ? theme.colors.onAccent : theme.colors.textPrimary)};
  background: ${({ $brand, theme }) => ($brand === 'primary' ? theme.colors.accent : 'transparent')};
  border: 1px solid ${({ $brand, theme }) => ($brand === 'primary' ? theme.colors.accent : theme.colors.border)};

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
  transition:
    transform ${({ theme }) => theme.motion.fast},
    filter ${({ theme }) => theme.motion.fast};

  &:active:not(:disabled) {
    transform: scale(0.98);
    filter: brightness(0.95);
  }
`;

const Divider = styled.p`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  margin-top: ${({ theme }) => theme.space(2)};
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textTertiary};

  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: ${({ theme }) => theme.colors.border};
  }
`;

const PointBadge = styled.span`
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 4px;
  padding: 5px 10px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 13px;
  font-weight: 800;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.accentSoft};
  font-variant-numeric: tabular-nums;
`;

const Version = styled.p`
  margin-top: ${({ theme }) => theme.space(10)};
  text-align: center;
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textTertiary};
`;
