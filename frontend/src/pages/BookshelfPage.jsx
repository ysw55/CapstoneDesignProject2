import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled, { css, keyframes } from 'styled-components';
import { BadgeCheck, ChevronRight, LibraryBig, LockKeyhole, Pencil, Plus, RefreshCw, Trash2 } from 'lucide-react';
import ScreenHeader from '../components/common/ScreenHeader';
import EmptyState from '../components/common/EmptyState';
import { IconButton } from '../components/common/IconButton';
import Button from '../components/common/Button';
import Toast from '../components/common/Toast';
import { ConfirmDialog } from '../components/common/Modal';
import BookCover from '../components/book/BookCover';
import LoadingScreen from '../components/common/LoadingScreen';
import { useAppData } from '../context/AppDataContext';
import { useAuth } from '../context/AuthContext';
import useFlashToast from '../hooks/useFlashToast';
import { formatDate, todayISO } from '../utils/format';

const BOOKS_PER_SHELF = 3;

// [a,b,c,d,e,f] → [[a,b,c],[d,e,f]] : 한 칸(선반)에 3권씩
const toShelves = (books) =>
  Array.from({ length: Math.ceil(books.length / BOOKS_PER_SHELF) }, (_, i) =>
    books.slice(i * BOOKS_PER_SHELF, (i + 1) * BOOKS_PER_SHELF)
  );

export default function BookshelfPage() {
  const navigate = useNavigate();
  const { books, shelfStatus, shelfError, reloadShelf, highlightBookId, highlightBookIsNew, deleteBook, clearHighlight } =
    useAppData();
  const { status: authStatus, openLogin } = useAuth();
  const [editMode, setEditMode] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [toast, showToast] = useFlashToast(clearHighlight);

  // 완독 날짜가 최근인 책이 앞에 오도록 (날짜를 수정하면 자리도 바뀜)
  const myBooks = useMemo(
    () => [...books].sort((a, b) => b.completedAt.localeCompare(a.completedAt) || b.id - a.id),
    [books]
  );

  const thisMonth = todayISO().slice(0, 7); // "2026-10"
  const thisMonthCount = myBooks.filter((b) => b.completedAt.startsWith(thisMonth)).length;
  const thisYearCount = myBooks.filter((b) => b.completedAt.startsWith(thisMonth.slice(0, 4))).length;
  const goAdd = () => navigate('/books/new');
  const certifiedCount = myBooks.filter((b) => b.certified).length;

  const confirmDelete = async () => {
    const target = deleteTarget;
    setDeleteTarget(null);
    try {
      await deleteBook(target.id);
      showToast('책장에서 뺐어요');
      if (books.length === 1) setEditMode(false); // 마지막 책이었으면 편집 모드 종료
    } catch (err) {
      showToast(err.message);
    }
  };

  const headerActions = (
    <>
      {shelfStatus === 'ready' && myBooks.length > 0 && (
        <EditToggle
          type="button"
          aria-pressed={editMode}
          $active={editMode}
          onClick={() => setEditMode((v) => !v)}
        >
          {editMode ? '완료' : '편집'}
        </EditToggle>
      )}
      {!editMode && shelfStatus !== 'guest' && (
        <IconButton aria-label="완독한 책 추가" onClick={goAdd}>
          <Plus size={24} strokeWidth={1.8} />
        </IconButton>
      )}
    </>
  );

  return (
    <>
      <ScreenHeader title="내 책장" subtitle="끝까지 함께한 책들" right={headerActions} />

      {authStatus === 'restoring' || shelfStatus === 'loading' ? (
        <LoadingScreen inline message="책장을 불러오는 중…" />
      ) : shelfStatus === 'guest' ? (
        // 로그아웃 상태: 다른 계정의 책장이 남지 않도록 비워 두고 로그인 안내
        <EmptyState
          icon={LockKeyhole}
          title="로그인하고 내 책장을 열어 보세요"
          description={'완독한 책과 인증 기록이 계정에 저장돼요.\n다른 기기에서도 그대로 볼 수 있어요.'}
          action={
            <Button $variant="primary" onClick={() => openLogin()}>
              로그인 / 회원가입
            </Button>
          }
        />
      ) : shelfStatus === 'error' ? (
        <EmptyState
          icon={RefreshCw}
          title="책장을 불러오지 못했어요"
          description={shelfError?.message}
          action={
            <Button $variant="secondary" onClick={reloadShelf}>
              다시 시도
            </Button>
          }
        />
      ) : myBooks.length === 0 ? (
        <EmptyState
          icon={LibraryBig}
          title="아직 꽂힌 책이 없어요"
          description={'완독한 책을 등록하면\n나만의 서재에 한 권씩 꽂혀요.'}
          action={
            <Button $variant="primary" onClick={goAdd}>
              <Plus size={18} strokeWidth={2.2} aria-hidden />
              책장에 추가하기
            </Button>
          }
        />
      ) : (
        <>
          <Stats>
            <Stat>
              <StatValue $accent>{myBooks.length}</StatValue>
              <StatLabel>총 완독</StatLabel>
            </Stat>
            <Stat>
              <StatValue>{thisYearCount}</StatValue>
              <StatLabel>올해</StatLabel>
            </Stat>
            <Stat>
              <StatValue>{thisMonthCount}</StatValue>
              <StatLabel>이번 달</StatLabel>
            </Stat>
          </Stats>

          {editMode ? (
            <EditHint>수정하거나 뺄 책의 버튼을 눌러 주세요 · 인증한 책은 표지 색만 바꿀 수 있어요</EditHint>
          ) : (
            <CertifyBanner type="button" onClick={() => navigate('/books/certify')}>
              <BadgeCheck size={22} strokeWidth={2} aria-hidden />
              <span>
                <strong>완독 인증하고 보상 받기</strong>
                독후감 200자 · 인증 {certifiedCount}/{myBooks.length}권
              </span>
              <ChevronRight size={20} strokeWidth={2} aria-hidden />
            </CertifyBanner>
          )}

          <Shelves>
            {toShelves(myBooks).map((shelf) => (
              <Shelf key={shelf[0].id}>
                <BookRow>
                  {shelf.map((book) => (
                    <BookSlot key={book.id} $new={book.id === highlightBookId}>
                      <Wiggle $on={editMode}>
                        <BookCover title={book.title} author={book.author} color={book.coverColor} />
                      </Wiggle>
                      {editMode && (
                        <CoverActions>
                          <CoverButton
                            type="button"
                            aria-label={`${book.title} 수정`}
                            onClick={() => navigate(`/books/${book.id}/edit`)}
                          >
                            <Pencil size={14} strokeWidth={2.2} />
                          </CoverButton>
                          {/* 완독 인증한 책은 보상 중복을 막기 위해 삭제 불가 (서버도 409 로 거절) */}
                          {!book.certified && (
                            <CoverButton
                              type="button"
                              aria-label={`${book.title} 삭제`}
                              $danger
                              onClick={() => setDeleteTarget(book)}
                            >
                              <Trash2 size={14} strokeWidth={2.2} />
                            </CoverButton>
                          )}
                        </CoverActions>
                      )}
                    </BookSlot>
                  ))}
                </BookRow>
                <Plank aria-hidden />
                <CaptionRow>
                  {shelf.map((book) => (
                    <Caption key={book.id}>
                      <CaptionTitle>
                        {highlightBookIsNew && book.id === highlightBookId && <NewBadge>NEW</NewBadge>}
                        {book.title}
                      </CaptionTitle>
                      <CaptionDate>
                        {book.certified && (
                          <CertifiedMark aria-label="완독 인증 완료">
                            <BadgeCheck size={12} strokeWidth={2.6} aria-hidden />
                          </CertifiedMark>
                        )}
                        {formatDate(book.completedAt)} 완독
                      </CaptionDate>
                    </Caption>
                  ))}
                </CaptionRow>
              </Shelf>
            ))}
          </Shelves>
        </>
      )}

      {toast && <Toast key={toast.key} message={toast.message} />}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="책장에서 뺄까요?"
        message={
          deleteTarget &&
          `『${deleteTarget.title}』을(를) 내 책장에서 삭제해요.\n커뮤니티에 공유한 글은 그대로 남아요.`
        }
        confirmLabel="삭제"
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </>
  );
}

const EditToggle = styled.button`
  align-self: center;
  height: 32px;
  padding: 0 12px;
  margin-right: 4px;
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 14px;
  font-weight: 700;
  color: ${({ $active, theme }) => ($active ? theme.colors.onAccent : theme.colors.textPrimary)};
  background: ${({ $active, theme }) => ($active ? theme.colors.accent : theme.colors.surfaceMuted)};
  transition:
    background ${({ theme }) => theme.motion.fast},
    transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.94);
  }
`;

const CertifyBanner = styled.button`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.space(3)};
  width: calc(100% - 40px);
  margin: -16px ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(6)};
  padding: 14px 16px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  text-align: left;
  background: ${({ theme }) => theme.colors.surface};
  transition:
    transform ${({ theme }) => theme.motion.fast},
    background ${({ theme }) => theme.motion.fast};

  > svg:first-child {
    flex-shrink: 0;
    color: ${({ theme }) => theme.colors.accent};
  }

  > svg:last-child {
    flex-shrink: 0;
    color: ${({ theme }) => theme.colors.textTertiary};
  }

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

  &:active {
    transform: scale(0.98);
    background: ${({ theme }) => theme.colors.accentSoft};
  }
`;

const CertifiedMark = styled.span`
  display: inline-grid;
  place-items: center;
  margin-right: 3px;
  vertical-align: -2px;
  color: ${({ theme }) => theme.colors.accent};
`;

const EditHint = styled.p`
  margin: -16px ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(5)};
  text-align: center;
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textTertiary};
`;

const Stats = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 0 ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(8)};
  padding: ${({ theme }) => theme.space(4)} 0;
  border-radius: ${({ theme }) => theme.radius.lg};
  background: ${({ theme }) => theme.colors.backgroundAlt};
`;

const Stat = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;

  & + & {
    border-left: 1px solid ${({ theme }) => theme.colors.border};
  }
`;

const StatValue = styled.span`
  font-size: ${({ theme }) => theme.fontSize.xl};
  font-weight: 800;
  letter-spacing: -0.03em;
  color: ${({ $accent, theme }) => ($accent ? theme.colors.accent : theme.colors.textPrimary)};
  font-variant-numeric: tabular-nums;
`;

const StatLabel = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.colors.textSecondary};
`;

const Shelves = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.space(8)};
  padding: 0 ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(8)};
`;

const Shelf = styled.section``;

const shelfGrid = `
  display: grid;
  grid-template-columns: repeat(${BOOKS_PER_SHELF}, minmax(0, 1fr)); /* 긴 저자명이 칸을 늘리지 않도록 */
  gap: 18px;
  padding: 0 12px;
`;

const BookRow = styled.div`
  ${shelfGrid}
  align-items: end;
`;

/* 방금 꽂히거나 수정된 책은 위에서 쏙 들어오는 애니메이션 */
const slideIn = keyframes`
  from { opacity: 0; transform: translateY(-24px) rotate(-4deg); }
  70%  { opacity: 1; transform: translateY(3px) rotate(0.5deg); }
  to   { transform: translateY(0) rotate(0); }
`;

/* 편집 모드: iOS 홈 화면처럼 살짝 흔들림 */
const wiggle = keyframes`
  0%, 100% { transform: rotate(-0.8deg); }
  50%      { transform: rotate(0.8deg); }
`;

const BookSlot = styled.div`
  position: relative;

  ${({ $new }) =>
    $new &&
    css`
      animation: ${slideIn} 600ms cubic-bezier(0.2, 0, 0, 1) 150ms both;
    `}

`;

/* 흔들리는 건 표지만 — 위에 얹힌 버튼은 고정돼 있어야 정확히 누를 수 있다 */
const Wiggle = styled.div`
  ${({ $on }) =>
    $on &&
    css`
      animation: ${wiggle} 260ms ease-in-out infinite;

      @media (prefers-reduced-motion: reduce) {
        animation: none;
      }
    `}
`;

/* 표지 우측 상단의 수정·삭제 버튼 */
const CoverActions = styled.div`
  position: absolute;
  top: -10px;
  right: -10px;
  display: flex;
  gap: 2px;
`;

const CoverButton = styled.button`
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 50%;
  color: ${({ $danger, theme }) => ($danger ? theme.colors.danger : theme.colors.accent)};
  background: ${({ theme }) => theme.colors.surface};
  box-shadow: 0 2px 6px rgba(17, 17, 17, 0.2);
  transition: transform ${({ theme }) => theme.motion.fast};

  &:active {
    transform: scale(0.88);
  }
`;

/* 책이 올라앉은 선반 */
const Plank = styled.div`
  height: 10px;
  border-radius: 2px 2px 4px 4px;
  background: linear-gradient(
    180deg,
    ${({ theme }) => theme.colors.surfaceMuted} 0%,
    ${({ theme }) => theme.colors.border} 100%
  );
  box-shadow: 0 6px 10px -6px rgba(17, 17, 17, 0.18);
`;

const CaptionRow = styled.div`
  ${shelfGrid}
  margin-top: ${({ theme }) => theme.space(3)};
`;

const Caption = styled.div`
  min-width: 0;
`;

const CaptionTitle = styled.p`
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 700;
  letter-spacing: -0.02em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const NewBadge = styled.span`
  display: inline-block;
  margin-right: 4px;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.04em;
  vertical-align: 2px;
  color: ${({ theme }) => theme.colors.onAccent};
  background: ${({ theme }) => theme.colors.accent};
`;

const CaptionDate = styled.p`
  font-size: 11px;
  color: ${({ theme }) => theme.colors.textTertiary};
  font-variant-numeric: tabular-nums;
`;
