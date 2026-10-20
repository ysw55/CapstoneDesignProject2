import React, { useState } from 'react';
import styled, { css, keyframes } from 'styled-components';
import { Bookmark, BookOpen, Heart, MessageCircle, Pencil, Trash2 } from 'lucide-react';
import { isMyPost, useAppData } from '../../context/AppDataContext';
import { useAuth } from '../../context/AuthContext';
import { formatCount } from '../../utils/format';
import ProfileAvatar from '../common/ProfileAvatar';
import CommentPanel from './CommentPanel';

/**
 * 커뮤니티 피드의 문장 카드 한 장.
 * - highlight: 방금 올리거나 수정한 글이면 강조
 * - onEdit / onDelete: 내가 쓴 글일 때만 우측 상단에 수정·삭제 버튼 표시
 */
export default function QuoteCard({ post, highlight = false, onEdit, onDelete }) {
  const { toggleLike } = useAppData();
  const { isLoggedIn, openLogin } = useAuth();
  const mine = isMyPost(post);

  const handleLike = () => {
    if (!isLoggedIn) {
      openLogin(); // 비로그인: 좋아요 대신 로그인 시트
      return;
    }
    // 실패하면 AppDataContext 가 원래 상태로 되돌림 — 화면에서 따로 할 일 없음
    toggleLike(post).catch(() => {});
  };
  const [saved, setSaved] = useState(false); // 저장(북마크)은 아직 내 기기 안에서만
  const [commentsOpen, setCommentsOpen] = useState(false);

  return (
    <Card $highlight={highlight}>
      <Header>
        <ProfileAvatar name={post.userName} background={post.userSkin?.background} border={post.userSkin?.border} size={36} />
        <Meta>
          <UserName>
            {post.userName}
            {mine && <MineBadge>나</MineBadge>}
          </UserName>
          <Time>
            {post.createdAt}
            {post.edited && ' · 수정됨'}
          </Time>
        </Meta>
        {mine && (
          <OwnerActions>
            <OwnerButton type="button" aria-label="글 수정" onClick={() => onEdit?.(post)}>
              <Pencil size={16} strokeWidth={2} />
            </OwnerButton>
            <OwnerButton type="button" aria-label="글 삭제" $danger onClick={() => onDelete?.(post)}>
              <Trash2 size={16} strokeWidth={2} />
            </OwnerButton>
          </OwnerActions>
        )}
      </Header>

      <TagChip>
        <span aria-hidden>{post.tag.emoji}</span> {post.tag.label}
      </TagChip>

      <Quote>{post.quote}</Quote>

      <BookLine>
        <BookOpen size={14} strokeWidth={2} aria-hidden />
        <strong>{post.bookTitle}</strong>
        <span>· {post.author}</span>
      </BookLine>

      <Actions>
        <ActionButton
          type="button"
          aria-pressed={post.liked}
          aria-label={post.liked ? '좋아요 취소' : '좋아요'}
          $active={post.liked}
          onClick={handleLike}
        >
          {/* key 를 바꿔 누를 때마다 팝 애니메이션을 다시 재생 */}
          <HeartIcon key={String(post.liked)} $pop={post.liked}>
            <Heart size={20} strokeWidth={1.8} fill={post.liked ? 'currentColor' : 'none'} />
          </HeartIcon>
          <CountText>{formatCount(post.likeCount)}</CountText>
        </ActionButton>

        <ActionButton
          type="button"
          aria-expanded={commentsOpen}
          aria-label="댓글"
          $active={commentsOpen}
          onClick={() => setCommentsOpen((v) => !v)}
        >
          <MessageCircle size={20} strokeWidth={1.8} fill={commentsOpen ? 'currentColor' : 'none'} fillOpacity={0.12} />
          <CountText>{formatCount(post.commentCount)}</CountText>
        </ActionButton>

        <ActionButton
          type="button"
          aria-pressed={saved}
          aria-label="문장 저장"
          $active={saved}
          $push
          onClick={() => setSaved((v) => !v)}
        >
          <Bookmark size={20} strokeWidth={1.8} fill={saved ? 'currentColor' : 'none'} />
        </ActionButton>
      </Actions>

      {commentsOpen && <CommentPanel post={post} />}
    </Card>
  );
}

const flash = keyframes`
  0%   { opacity: 0; transform: translateY(-12px); }
  60%  { opacity: 1; transform: translateY(0); }
`;

const pop = keyframes`
  0%   { transform: scale(1); }
  35%  { transform: scale(1.3); }
  100% { transform: scale(1); }
`;

const Card = styled.article`
  padding: ${({ theme }) => `${theme.space(5)} ${theme.space(5)} ${theme.space(3)}`};
  border: 1px solid ${({ $highlight, theme }) => ($highlight ? theme.colors.accent : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.lg};
  background: ${({ theme }) => theme.colors.surface};
  transition: border-color 1.2s ease;

  ${({ $highlight }) =>
    $highlight &&
    css`
      animation: ${flash} 500ms cubic-bezier(0.2, 0, 0, 1);
    `}
`;

const Header = styled.header`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
`;

const Meta = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
`;

const UserName = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.02em;
`;

const Time = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textTertiary};
`;

const MineBadge = styled.span`
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 800;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.accentSoft};
`;

const OwnerActions = styled.div`
  display: flex;
  gap: 2px;
  margin-right: -8px;
`;

const OwnerButton = styled.button`
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: ${({ theme }) => theme.colors.textTertiary};
  transition:
    color ${({ theme }) => theme.motion.fast},
    background ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  &:hover,
  &:focus-visible {
    color: ${({ $danger, theme }) => ($danger ? theme.colors.danger : theme.colors.accent)};
  }

  &:active {
    transform: scale(0.9);
    color: ${({ $danger, theme }) => ($danger ? theme.colors.danger : theme.colors.accent)};
    background: ${({ $danger, theme }) => ($danger ? theme.colors.dangerSoft : theme.colors.accentSoft)};
  }
`;

const TagChip = styled.span`
  display: inline-block;
  margin-top: ${({ theme }) => theme.space(4)};
  padding: 5px 10px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.colors.textSecondary};
  background: ${({ theme }) => theme.colors.backgroundAlt};
  white-space: nowrap;
`;

const Quote = styled.blockquote`
  margin: ${({ theme }) => `${theme.space(3)} 0 ${theme.space(4)}`};
  padding-left: ${({ theme }) => theme.space(4)};
  border-left: 2px solid ${({ theme }) => theme.colors.accent};
  font-family: ${({ theme }) => theme.font.serif};
  font-size: 17px;
  line-height: 1.75;
  letter-spacing: -0.02em;
  word-break: keep-all;
  white-space: pre-line;
`;

const BookLine = styled.p`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: ${({ theme }) => theme.fontSize.sm};
  color: ${({ theme }) => theme.colors.textSecondary};

  svg {
    flex-shrink: 0;
    color: ${({ theme }) => theme.colors.accent};
  }

  strong {
    font-weight: 700;
    color: ${({ theme }) => theme.colors.textPrimary};
  }

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

const Actions = styled.div`
  display: flex;
  align-items: center;
  margin-top: ${({ theme }) => theme.space(3)};
  padding-top: ${({ theme }) => theme.space(2)};
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;

const ActionButton = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 ${({ theme }) => theme.space(2)};
  margin-left: ${({ $push }) => ($push ? 'auto' : '0')};
  margin-right: ${({ $push }) => ($push ? '-8px' : '8px')};
  &:first-child { margin-left: -8px; }
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 600;
  color: ${({ $active, theme }) => ($active ? theme.colors.accent : theme.colors.textSecondary)};
  transition:
    color ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.9);
  }
`;

const HeartIcon = styled.span`
  display: inline-flex;
  ${({ $pop }) =>
    $pop &&
    css`
      animation: ${pop} 350ms cubic-bezier(0.3, 1.4, 0.5, 1);
    `}
`;

const CountText = styled.span`
  min-width: 2ch;
  text-align: left;
  font-variant-numeric: tabular-nums;
`;
