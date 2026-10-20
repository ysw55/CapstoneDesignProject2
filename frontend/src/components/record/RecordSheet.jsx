import React from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { BadgeCheck, ChevronRight, Coins, Lock, LibraryBig, MessageSquareQuote, Globe } from 'lucide-react';
import { BottomSheet } from '../common/Modal';

const OPTIONS = [
  {
    to: '/books/new',
    icon: LibraryBig,
    title: '책장에 추가',
    description: '다 읽은 책을 나만의 서재에 꽂아요',
    badge: { icon: Lock, label: '나만 보기' },
  },
  {
    to: '/posts/new',
    icon: MessageSquareQuote,
    title: '커뮤니티에 공유',
    description: '마음에 남은 문장을 사람들과 나눠요',
    badge: { icon: Globe, label: '전체 공개' },
  },
  {
    to: '/books/certify',
    icon: BadgeCheck,
    title: '완독 인증',
    description: '독후감 200자를 쓰고 보상 스킨을 받아요',
    badge: { icon: Coins, label: '+100P' },
  },
];

/** '기록하기' 버튼을 누르면 열리는 선택 시트 */
export default function RecordSheet({ open, onClose }) {
  const navigate = useNavigate();

  return (
    <BottomSheet open={open} onClose={onClose} title="무엇을 기록할까요?">
      <Options>
        {OPTIONS.map(({ to, icon: Icon, title, description, badge: { icon: BadgeIcon, label } }) => (
          <li key={to}>
            <Option
              type="button"
              onClick={() => {
                onClose();
                navigate(to);
              }}
            >
              <IconTile>
                <Icon size={24} strokeWidth={1.8} aria-hidden />
              </IconTile>
              <Text>
                <TitleRow>
                  <strong>{title}</strong>
                  <Badge>
                    <BadgeIcon size={11} strokeWidth={2.4} aria-hidden />
                    {label}
                  </Badge>
                </TitleRow>
                <span>{description}</span>
              </Text>
              <ChevronRight size={20} strokeWidth={2} aria-hidden />
            </Option>
          </li>
        ))}
      </Options>
    </BottomSheet>
  );
}

const Options = styled.ul`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(3)};
`;

const Option = styled.button`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(4)};
  width: 100%;
  padding: ${({ theme }) => theme.space(4)};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  text-align: left;
  background: ${({ theme }) => theme.colors.surface};
  transition:
    background ${({ theme }) => theme.motion.fast},
    border-color ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  > svg {
    flex-shrink: 0;
    color: ${({ theme }) => theme.colors.textTertiary};
  }

  &:active {
    transform: scale(0.98);
    border-color: ${({ theme }) => theme.colors.accent};
    background: ${({ theme }) => theme.colors.accentSoft};
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.accent};
    outline-offset: 2px;
  }
`;

const IconTile = styled.span`
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  color: ${({ theme }) => theme.colors.onAccent};
  background: ${({ theme }) => theme.colors.accent};
`;

const Text = styled.span`
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 2px;
  min-width: 0;

  > span {
    font-size: 13px;
    color: ${({ theme }) => theme.colors.textSecondary};
  }
`;

const TitleRow = styled.span`
  display: flex;
  align-items: center;
  gap: 6px;

  strong {
    font-size: 16px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
`;

const Badge = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 6px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 10.5px;
  font-weight: 700;
  color: ${({ theme }) => theme.colors.textSecondary};
  background: ${({ theme }) => theme.colors.surfaceMuted};
`;
