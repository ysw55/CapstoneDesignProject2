import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { Bell, PenLine, RefreshCw } from 'lucide-react';
import ScreenHeader from '../components/common/ScreenHeader';
import { IconButton } from '../components/common/IconButton';
import Toast from '../components/common/Toast';
import { ConfirmDialog } from '../components/common/Modal';
import QuoteCard from '../components/feed/QuoteCard';
import RecordSheet from '../components/record/RecordSheet';
import LoadingScreen from '../components/common/LoadingScreen';
import EmptyState from '../components/common/EmptyState';
import Button from '../components/common/Button';
import { useAppData } from '../context/AppDataContext';
import { useAuth } from '../context/AuthContext';
import useFlashToast from '../hooks/useFlashToast';

const ALL = '전체';
const tagKey = (tag) => `${tag.emoji} ${tag.label}`;

export default function CommunityPage() {
  const navigate = useNavigate();
  const { posts, feedStatus, feedError, reloadFeed, highlightPostId, deletePost, clearHighlight } = useAppData();
  const { isLoggedIn, openLogin } = useAuth();
  const [activeTag, setActiveTag] = useState(ALL);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [toast, showToast] = useFlashToast(clearHighlight);

  // 피드에 있는 상황 태그를 중복 없이 뽑아 필터 칩으로 사용
  const tags = useMemo(() => [ALL, ...new Set(posts.map((p) => tagKey(p.tag)))], [posts]);
  // 글이 지워져 선택된 태그가 사라지면 '전체'로
  const currentTag = tags.includes(activeTag) ? activeTag : ALL;
  const visiblePosts = currentTag === ALL ? posts : posts.filter((p) => tagKey(p.tag) === currentTag);

  const confirmDelete = async () => {
    const target = deleteTarget;
    setDeleteTarget(null);
    try {
      await deletePost(target.id);
      showToast('글을 삭제했어요');
    } catch (err) {
      showToast(err.message);
    }
  };

  // 기록하기: 비로그인이면 로그인 시트부터
  const handleRecord = () => (isLoggedIn ? setSheetOpen(true) : openLogin());

  return (
    <>
      <ScreenHeader
        title="Book Canvas"
        subtitle="오늘, 누군가의 마음에 남은 한 문장"
        right={
          <IconButton aria-label="알림">
            <Bell size={22} strokeWidth={1.8} />
          </IconButton>
        }
      />

      <ChipScroller role="tablist" aria-label="상황 태그">
        {tags.map((tag) => (
          <Chip
            key={tag}
            type="button"
            role="tab"
            aria-selected={currentTag === tag}
            $active={currentTag === tag}
            onClick={() => setActiveTag(tag)}
          >
            {tag}
          </Chip>
        ))}
      </ChipScroller>

      {feedStatus === 'loading' && <LoadingScreen inline message="피드를 불러오는 중…" />}
      {feedStatus === 'error' && (
        <EmptyState
          icon={RefreshCw}
          title="피드를 불러오지 못했어요"
          description={feedError?.message}
          action={
            <Button $variant="secondary" onClick={reloadFeed}>
              다시 시도
            </Button>
          }
        />
      )}

      <Feed>
        {visiblePosts.map((post) => (
          <li key={post.id}>
            <QuoteCard
              post={post}
              highlight={post.id === highlightPostId}
              onEdit={(p) => navigate(`/posts/${p.id}/edit`)}
              onDelete={setDeleteTarget}
            />
          </li>
        ))}
        {feedStatus === 'ready' && visiblePosts.length === 0 && <EmptyFeed>아직 공유된 문장이 없어요. 첫 문장을 남겨 보세요!</EmptyFeed>}
      </Feed>

      {toast ? (
        <Toast key={toast.key} message={toast.message} />
      ) : (
        <Fab type="button" aria-label="기록하기" aria-haspopup="dialog" onClick={handleRecord}>
          <PenLine size={20} strokeWidth={2} aria-hidden />
          기록하기
        </Fab>
      )}

      <RecordSheet open={sheetOpen} onClose={() => setSheetOpen(false)} />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="이 글을 삭제할까요?"
        message={'삭제한 글과 댓글은 되돌릴 수 없어요.'}
        confirmLabel="삭제"
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </>
  );
}

const ChipScroller = styled.div`
  display: flex;
  gap: ${({ theme }) => theme.space(2)};
  padding: ${({ theme }) => `0 ${theme.layout.gutter} ${theme.space(4)}`};
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
`;

const Chip = styled.button`
  flex-shrink: 0;
  height: 36px;
  padding: 0 14px;
  border: 1px solid ${({ $active, theme }) => ($active ? theme.colors.textPrimary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 600;
  white-space: nowrap;
  color: ${({ $active, theme }) => ($active ? theme.colors.background : theme.colors.textSecondary)};
  background: ${({ $active, theme }) => ($active ? theme.colors.textPrimary : theme.colors.surface)};
  transition:
    background ${({ theme }) => theme.motion.fast},
    color ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.95);
  }
`;

const Feed = styled.ul`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(3)};
  padding: 0 ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(20)};
`;

const EmptyFeed = styled.li`
  padding: ${({ theme }) => theme.space(16)} 0;
  text-align: center;
  font-size: 14px;
  color: ${({ theme }) => theme.colors.textTertiary};
`;

/* 탭 바 위에 떠 있는 글쓰기 버튼. 데스크톱에서도 430px 프레임 안쪽에 붙는다. */
const Fab = styled.button`
  position: fixed;
  z-index: 90;
  right: max(20px, calc((100vw - ${({ theme }) => theme.layout.maxWidth}) / 2 + 20px));
  bottom: calc(${({ theme }) => theme.layout.tabBarHeight} + env(safe-area-inset-bottom, 0px) + 16px);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 52px;
  padding: 0 20px 0 18px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: ${({ theme }) => theme.fontSize.md};
  font-weight: 700;
  color: ${({ theme }) => theme.colors.onAccent};
  background: ${({ theme }) => theme.colors.accent};
  box-shadow: 0 8px 24px -6px rgba(27, 77, 62, 0.5);
  transition: transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.95);
    background: ${({ theme }) => theme.colors.accentPressed};
  }
`;
