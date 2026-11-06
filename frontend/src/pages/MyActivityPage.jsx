import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { Bookmark, ChevronLeft, Heart, MessageCircle, RefreshCw } from 'lucide-react';
import EmptyState from '../components/common/EmptyState';
import Button from '../components/common/Button';
import LoadingScreen from '../components/common/LoadingScreen';
import Toast from '../components/common/Toast';
import { mapQuote, useAppData } from '../context/AppDataContext';
import useFlashToast from '../hooks/useFlashToast';
import * as quotesApi from '../api/quotes';
import { relativeTime } from '../utils/format';

const PAGE_SIZE = 20;

// 탭마다 다른 부분을 한곳에 모아 둔다
const TABS = {
  liked: {
    label: '좋아요',
    icon: Heart,
    fetch: quotesApi.fetchMyLiked,
    undo: (quote) => quotesApi.unlikeQuote(quote.id),
    undoLabel: '좋아요 취소',
    undoneToast: '좋아요를 취소했어요',
    actedLabel: '좋아요',
    emptyTitle: '아직 좋아요한 문장이 없어요',
    emptyDescription: '커뮤니티에서 마음에 남는 문장에\n하트를 눌러 보세요.',
    feedPatch: (quote) => ({ liked: false, likeCount: Math.max(quote.likeCount - 1, 0) }),
  },
  bookmarked: {
    label: '저장',
    icon: Bookmark,
    fetch: quotesApi.fetchMyBookmarked,
    undo: (quote) => quotesApi.unbookmarkQuote(quote.id),
    undoLabel: '저장 취소',
    undoneToast: '저장을 취소했어요',
    actedLabel: '저장',
    emptyTitle: '아직 저장한 문장이 없어요',
    emptyDescription: '나중에 다시 읽고 싶은 문장은\n북마크로 저장해 두세요.',
    feedPatch: () => ({ bookmarked: false }),
  },
};

const emptyList = { items: [], page: 0, last: true, total: 0, status: 'loading', error: null };

/** 프로필 > 좋아요 · 저장한 문장 (/profile/activity) — 로그인 필수(RequireAuth) */
export default function MyActivityPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { patchFeedPost } = useAppData();
  const [toast, showToast] = useFlashToast();
  const [tab, setTab] = useState('liked');
  const [lists, setLists] = useState({ liked: emptyList, bookmarked: emptyList });
  const [loadingMore, setLoadingMore] = useState(false);
  const requestSeq = useRef({ liked: 0, bookmarked: 0 }); // 늦게 도착한 이전 응답을 버리기 위한 번호

  const setList = useCallback((key, patch) => setLists((prev) => ({ ...prev, [key]: { ...prev[key], ...patch } })), []);

  const load = useCallback(
    async (key, page) => {
      const seq = ++requestSeq.current[key];
      try {
        const res = await TABS[key].fetch({ page, size: PAGE_SIZE });
        if (seq !== requestSeq.current[key]) return;
        const items = res.content.map((r) => ({ quote: mapQuote(r.quote), actedAt: r.actedAt }));
        setLists((prev) => ({
          ...prev,
          [key]: {
            items: page === 0 ? items : [...prev[key].items, ...items],
            page: res.pageNumber,
            last: res.last,
            total: res.totalElements,
            status: 'ready',
            error: null,
          },
        }));
      } catch (error) {
        if (seq === requestSeq.current[key]) setList(key, { status: 'error', error });
        throw error;
      }
    },
    [setList]
  );

  // 두 탭의 첫 페이지를 함께 불러와 탭에 개수를 보여 준다
  useEffect(() => {
    load('liked', 0).catch(() => {});
    load('bookmarked', 0).catch(() => {});
  }, [load]);

  const reload = (key) => {
    setList(key, { status: 'loading', error: null });
    load(key, 0).catch(() => {});
  };

  const loadMore = async () => {
    setLoadingMore(true);
    try {
      await load(tab, lists[tab].page + 1);
    } catch (err) {
      showToast(err.message);
    } finally {
      setLoadingMore(false);
    }
  };

  const undo = async (item) => {
    const cfg = TABS[tab];
    try {
      await cfg.undo(item.quote);
      setLists((prev) => ({
        ...prev,
        [tab]: {
          ...prev[tab],
          items: prev[tab].items.filter((i) => i.quote.id !== item.quote.id),
          total: Math.max(prev[tab].total - 1, 0),
        },
      }));
      patchFeedPost(item.quote.id, cfg.feedPatch(item.quote)); // 커뮤니티 피드에 떠 있는 같은 글도 함께 맞춘다
      showToast(cfg.undoneToast);
    } catch (err) {
      showToast(err.message);
    }
  };

  const handleBack = () => (location.key === 'default' ? navigate('/profile', { replace: true }) : navigate(-1));
  const current = lists[tab];
  const cfg = TABS[tab];

  return (
    <>
      <TopBar>
        <IconBtn type="button" aria-label="뒤로 가기" onClick={handleBack}>
          <ChevronLeft size={26} strokeWidth={2} />
        </IconBtn>
        <TopTitle>좋아요 · 저장한 문장</TopTitle>
        <span aria-hidden style={{ width: 44 }} />
      </TopBar>

      <Tabs role="tablist">
        {Object.entries(TABS).map(([key, t]) => (
          <Tab
            key={key}
            type="button"
            role="tab"
            aria-selected={tab === key}
            $active={tab === key}
            onClick={() => setTab(key)}
          >
            <t.icon size={16} strokeWidth={2.2} aria-hidden />
            {t.label}
            <em>{lists[key].status === 'ready' ? lists[key].total : ''}</em>
          </Tab>
        ))}
      </Tabs>

      {current.status === 'loading' ? (
        <LoadingScreen inline message="불러오는 중…" />
      ) : current.status === 'error' ? (
        <EmptyState
          icon={RefreshCw}
          title="불러오지 못했어요"
          description={current.error?.message}
          action={
            <Button $variant="secondary" onClick={() => reload(tab)}>
              다시 시도
            </Button>
          }
        />
      ) : current.items.length === 0 ? (
        <EmptyState
          icon={cfg.icon}
          title={cfg.emptyTitle}
          description={cfg.emptyDescription}
          action={
            <Button $variant="primary" onClick={() => navigate('/')}>
              커뮤니티 둘러보기
            </Button>
          }
        />
      ) : (
        <List>
          {current.items.map(({ quote, actedAt }) => (
            <Item key={quote.id}>
              <Quote>{quote.quote}</Quote>
              <BookLine>
                {quote.bookTitle} · {quote.author}
              </BookLine>
              <Footer>
                <Meta>
                  <strong>{quote.userName}</strong>
                  <span>
                    {relativeTime(actedAt)} {cfg.actedLabel}
                  </span>
                  <span>
                    <MessageCircle size={12} strokeWidth={2} aria-hidden /> {quote.commentCount}
                  </span>
                </Meta>
                <UndoButton type="button" aria-label={cfg.undoLabel} onClick={() => undo({ quote, actedAt })}>
                  <cfg.icon size={18} strokeWidth={1.8} fill="currentColor" aria-hidden />
                  {cfg.undoLabel}
                </UndoButton>
              </Footer>
            </Item>
          ))}
          {!current.last && (
            <MoreWrap>
              <Button $variant="secondary" $block disabled={loadingMore} onClick={loadMore}>
                {loadingMore ? '불러오는 중…' : '더 보기'}
              </Button>
            </MoreWrap>
          )}
        </List>
      )}

      {toast && <Toast key={toast.key} message={toast.message} />}
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

const Tabs = styled.div`
  display: flex;
  gap: ${({ theme }) => theme.space(1)};
  margin: ${({ theme }) => theme.space(2)} ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(4)};
  padding: 3px;
  border-radius: ${({ theme }) => theme.radius.pill};
  background: ${({ theme }) => theme.colors.surfaceMuted};
`;

const Tab = styled.button`
  display: inline-flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 36px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 700;
  color: ${({ $active, theme }) => ($active ? theme.colors.textPrimary : theme.colors.textTertiary)};
  background: ${({ $active, theme }) => ($active ? theme.colors.surface : 'transparent')};
  box-shadow: ${({ $active, theme }) => ($active ? theme.shadow.card : 'none')};
  transition:
    background ${({ theme }) => theme.motion.fast},
    color ${({ theme }) => theme.motion.fast};

  em {
    font-style: normal;
    font-variant-numeric: tabular-nums;
    color: ${({ $active, theme }) => ($active ? theme.colors.accent : 'inherit')};
  }
`;

const List = styled.ul`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(3)};
  padding: 0 ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(8)};
  list-style: none;
`;

const Item = styled.li`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(2)};
  padding: ${({ theme }) => theme.space(4)};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  background: ${({ theme }) => theme.colors.surface};
`;

const Quote = styled.p`
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 5;
  font-family: ${({ theme }) => theme.font.serif};
  font-size: ${({ theme }) => theme.fontSize.md};
  line-height: 1.7;
  word-break: keep-all;
  white-space: pre-line;
`;

const BookLine = styled.p`
  font-size: 12.5px;
  font-weight: 600;
  color: ${({ theme }) => theme.colors.accent};
`;

const Footer = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.space(2)};
  margin-top: ${({ theme }) => theme.space(1)};
`;

const Meta = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px ${({ theme }) => theme.space(2)};
  min-width: 0;
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textTertiary};

  strong {
    font-weight: 700;
    color: ${({ theme }) => theme.colors.textSecondary};
  }

  span {
    display: inline-flex;
    align-items: center;
    gap: 3px;
  }
`;

const UndoButton = styled.button`
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 12px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 12.5px;
  font-weight: 700;
  color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.accentSoft};
  transition: transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.94);
  }
`;

const MoreWrap = styled.li`
  margin-top: ${({ theme }) => theme.space(2)};
`;
