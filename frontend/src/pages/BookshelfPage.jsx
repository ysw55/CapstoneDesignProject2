import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled, { css, keyframes } from 'styled-components';
import {
  DndContext,
  MouseSensor,
  TouchSensor,
  closestCenter,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { SortableContext, arrayMove, rectSortingStrategy, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { BadgeCheck, ChevronRight, LibraryBig, LockKeyhole, Pencil, Plus, RefreshCw, Trash2 } from 'lucide-react';
import ScreenHeader from '../components/common/ScreenHeader';
import EmptyState from '../components/common/EmptyState';
import { IconButton } from '../components/common/IconButton';
import Button from '../components/common/Button';
import Toast from '../components/common/Toast';
import { BottomSheet, ConfirmDialog } from '../components/common/Modal';
import BookCover from '../components/book/BookCover';
import BookSpine from '../components/book/BookSpine';
import ShelfOrnament from '../components/book/ShelfOrnament';
import LoadingScreen from '../components/common/LoadingScreen';
import { compareShelf, useAppData } from '../context/AppDataContext';
import { useAuth } from '../context/AuthContext';
import useFlashToast from '../hooks/useFlashToast';
import { formatDate, todayISO } from '../utils/format';

// true: 책등(옆면)이 보이게 꽂기 / false: 표지가 정면으로 보이게 진열
const SPINE_VIEW = true;

// 표지 진열은 한 칸에 3권, 책등은 얇아서 7권
const BOOKS_PER_SHELF = SPINE_VIEW ? 7 : 3;

// [a,b,c,d,e,f] → [[a,b,c],[d,e,f]] : 한 칸(선반)에 BOOKS_PER_SHELF 권씩
const toShelves = (books) =>
  Array.from({ length: Math.ceil(books.length / BOOKS_PER_SHELF) }, (_, i) =>
    books.slice(i * BOOKS_PER_SHELF, (i + 1) * BOOKS_PER_SHELF)
  );

/** 편집 모드에서 끌어서 옮길 수 있는 칸. 움직이는 건 이 바깥 칸이라 BookSlot 의 등장 애니메이션과 겹치지 않는다 */
function SortableItem({ id, disabled, children }) {
  const { setNodeRef, attributes, listeners, transform, transition, isDragging } = useSortable({ id, disabled });
  return (
    <SortableCell
      ref={setNodeRef}
      $dragging={isDragging}
      style={{ transform: CSS.Translate.toString(transform), transition }}
    >
      {children(disabled ? {} : { ...attributes, ...listeners })}
    </SortableCell>
  );
}

export default function BookshelfPage() {
  const navigate = useNavigate();
  const { books, shelfStatus, shelfError, reloadShelf, highlightBookId, highlightBookIsNew, deleteBook, clearHighlight, reorderBooks } =
    useAppData();
  const { status: authStatus, openLogin } = useAuth();
  const [editMode, setEditMode] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [detailBook, setDetailBook] = useState(null); // 눌러서 상세를 보고 있는 책
  const [toast, showToast] = useFlashToast(clearHighlight);

  // 직접 정한 순서대로, 정한 적 없는 책은 맨 앞에 최근 완독 순으로 (서버와 같은 규칙)
  const myBooks = useMemo(() => [...books].sort(compareShelf), [books]);

  // 마우스는 살짝 움직이면 시작, 터치는 꾹 눌러야 시작 (그래야 스크롤과 구분됨)
  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 6 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 180, tolerance: 6 } })
  );

  const handleDragEnd = ({ active, over }) => {
    if (!over || active.id === over.id) return;
    const ids = myBooks.map((b) => b.id);
    const next = arrayMove(ids, ids.indexOf(active.id), ids.indexOf(over.id));
    reorderBooks(next).catch((err) => showToast(err.message));
  };

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
          onClick={() => {
            setDetailBook(null);
            setEditMode((v) => !v);
          }}
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
            <EditHint>
              책을 끌어서 순서를 바꾸고, 버튼으로 수정하거나 뺄 수 있어요 · 인증한 책은 표지 색만 바꿀 수 있어요
            </EditHint>
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
            <Crown aria-hidden />
            <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
              <SortableContext items={myBooks.map((b) => b.id)} strategy={rectSortingStrategy}>
                <Cabinet>
                  <Keystone aria-hidden />
                  <ShelfOrnament side="left" />
                  <ShelfOrnament side="right" />
                  <PillarBlock $side="left" aria-hidden />
                  <PillarBlock $side="right" aria-hidden />
                  <PillarBlock $side="left" $bottom aria-hidden />
                  <PillarBlock $side="right" $bottom aria-hidden />
                  {toShelves(myBooks).map((shelf) => (
                    <Shelf key={shelf[0].id}>
                      <BookRow $spine={SPINE_VIEW}>
                        {shelf.map((book) => (
                          <SortableItem key={book.id} id={book.id} disabled={!editMode}>
                            {(dragProps) => (
                              <BookSlot
                                $new={book.id === highlightBookId}
                                title={SPINE_VIEW ? `${book.title} · ${formatDate(book.completedAt)} 완독` : undefined}
                              >
                                {SPINE_VIEW && highlightBookIsNew && book.id === highlightBookId && (
                                  <SpineNew>NEW</SpineNew>
                                )}
                                <DragArea
                                  $on={editMode}
                                  {...(editMode
                                    ? dragProps
                                    : {
                                        role: 'button',
                                        tabIndex: 0,
                                        'aria-label': `${book.title} 상세 보기`,
                                        onClick: () => setDetailBook(book),
                                        onKeyDown: (e) => e.key === 'Enter' && setDetailBook(book),
                                      })}
                                >
                                  <Wiggle $on={editMode}>
                                    {SPINE_VIEW ? (
                                      <BookSpine
                                        title={book.title}
                                        author={book.author}
                                        color={book.coverColor}
                                        seed={book.id}
                                        certified={book.certified}
                                      />
                                    ) : (
                                      <BookCover title={book.title} author={book.author} color={book.coverColor} />
                                    )}
                                  </Wiggle>
                                </DragArea>
                                {editMode && (
                                  <CoverActions $below={SPINE_VIEW}>
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
                            )}
                          </SortableItem>
                        ))}
                      </BookRow>
                      <Plank aria-hidden />
                      {SPINE_VIEW ? (
                        // 책등에 제목이 있으니 캡션은 없다. 편집 모드에서만 버튼이 놓일 자리를 남긴다
                        <SpineFooter $editing={editMode} />
                      ) : (
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
                      )}
                    </Shelf>
                  ))}
                </Cabinet>
              </SortableContext>
            </DndContext>
            <Plinth aria-hidden />
          </Shelves>
        </>
      )}

      {toast && <Toast key={toast.key} message={toast.message} />}

      <BottomSheet open={Boolean(detailBook)} onClose={() => setDetailBook(null)} title="책 정보">
        {detailBook && (
          <Detail>
            <DetailCover>
              <BookCover title={detailBook.title} author={detailBook.author} color={detailBook.coverColor} size="lg" />
            </DetailCover>
            <DetailInfo>
              <DetailTitle>{detailBook.title}</DetailTitle>
              <DetailAuthor>{detailBook.author}</DetailAuthor>
              <DetailRows>
                <dt>완독 날짜</dt>
                <dd>{formatDate(detailBook.completedAt)}</dd>
                <dt>완독 인증</dt>
                <dd>
                  {detailBook.certified ? (
                    <DetailCertified>
                      <BadgeCheck size={14} strokeWidth={2.4} aria-hidden /> 인증 완료
                    </DetailCertified>
                  ) : (
                    '아직 인증 전'
                  )}
                </dd>
              </DetailRows>
            </DetailInfo>
          </Detail>
        )}
        {detailBook && (
          <DetailActions>
            <Button $variant="secondary" onClick={() => navigate(`/books/${detailBook.id}/edit`)}>
              <Pencil size={16} strokeWidth={2.2} aria-hidden />
              수정
            </Button>
            {!detailBook.certified && (
              <Button $variant="primary" onClick={() => navigate(`/books/certify?bookId=${detailBook.id}`)}>
                <BadgeCheck size={18} strokeWidth={2.2} aria-hidden />
                완독 인증하기
              </Button>
            )}
          </DetailActions>
        )}
      </BottomSheet>

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

const Detail = styled.div`
  display: flex;
  gap: ${({ theme }) => theme.space(4)};
  align-items: flex-start;
`;

const DetailCover = styled.div`
  flex-shrink: 0;
`;

const DetailInfo = styled.div`
  min-width: 0;
  flex: 1;
`;

const DetailTitle = styled.h3`
  font-family: ${({ theme }) => theme.font.serif};
  font-size: ${({ theme }) => theme.fontSize.lg};
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.02em;
  word-break: keep-all;
`;

const DetailAuthor = styled.p`
  margin-top: 2px;
  font-size: ${({ theme }) => theme.fontSize.sm};
  color: ${({ theme }) => theme.colors.textSecondary};
`;

const DetailRows = styled.dl`
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 6px ${({ theme }) => theme.space(3)};
  margin-top: ${({ theme }) => theme.space(4)};
  font-size: ${({ theme }) => theme.fontSize.sm};

  dt {
    color: ${({ theme }) => theme.colors.textTertiary};
  }

  dd {
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
`;

const DetailCertified = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: ${({ theme }) => theme.colors.accent};
`;

const DetailActions = styled.div`
  display: flex;
  gap: ${({ theme }) => theme.space(2)};
  margin-top: ${({ theme }) => theme.space(6)};

  > button {
    flex: 1;
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

/* 어두운 원목 위에서도 보이도록 딥그린 대신 밝은 금색 */
const CertifiedMark = styled.span`
  display: inline-grid;
  place-items: center;
  margin-right: 3px;
  vertical-align: -2px;
  color: ${({ theme }) => theme.furniture.trim};
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

const PILLAR = '26px';

const Shelves = styled.div`
  padding: 0 ${({ theme }) => theme.layout.gutter} ${({ theme }) => theme.space(8)};
`;

/* 책장 맨 위 상판 + 몰딩 */
const Crown = styled.div`
  position: relative;
  height: 24px;
  margin: 0 -6px;
  border-radius: 3px 3px 0 0;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent 40%),
    linear-gradient(
      180deg,
      ${({ theme }) => theme.furniture.top} 0 40%,
      ${({ theme }) => theme.furniture.line} 40% 48%,
      ${({ theme }) => theme.furniture.frame} 48%
    );
  border-bottom: 2px solid ${({ theme }) => theme.furniture.line};
  box-shadow: 0 0 0 1px ${({ theme }) => theme.furniture.line};

  /* 몰딩의 이빨(덴틸) 무늬 */
  &::after {
    content: '';
    position: absolute;
    right: 6px;
    bottom: 2px;
    left: 6px;
    height: 5px;
    background: repeating-linear-gradient(
      90deg,
      ${({ theme }) => theme.furniture.groove} 0 5px,
      transparent 5px 9px
    );
  }
`;

/* 검은 프레임. 양옆 기둥에는 세로 홈(플루팅)을 판다 */
const Cabinet = styled.div`
  position: relative;
  padding: 34px ${PILLAR} 0;
  background: ${({ theme }) => theme.furniture.frame};
  box-shadow: 0 0 0 1px ${({ theme }) => theme.furniture.line};

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 40px;
    bottom: 12px;
    width: ${PILLAR};
    border-block: 2px solid ${({ theme }) => theme.furniture.groove};
    background: linear-gradient(
      90deg,
      transparent 0 18%,
      ${({ theme }) => theme.furniture.groove} 18% 28%,
      transparent 28% 44%,
      ${({ theme }) => theme.furniture.groove} 44% 54%,
      transparent 54% 70%,
      ${({ theme }) => theme.furniture.groove} 70% 80%,
      transparent 80%
    );
    pointer-events: none;
  }

  &::before {
    left: 0;
  }

  &::after {
    right: 0;
  }
`;

/* 상단 중앙 장식 */
const Keystone = styled.div`
  position: absolute;
  top: 6px;
  left: 50%;
  width: 22px;
  height: 18px;
  transform: translateX(-50%);
  border-radius: 3px;
  background: linear-gradient(
    180deg,
    ${({ theme }) => theme.furniture.trim},
    ${({ theme }) => theme.furniture.woodDark}
  );
  border: 1px solid ${({ theme }) => theme.furniture.line};
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
`;

/* 기둥 머리(위)와 받침(아래) 블록 */
const PillarBlock = styled.div`
  position: absolute;
  ${({ $bottom }) => ($bottom ? 'bottom: 0; height: 14px;' : 'top: 6px; height: 28px;')}
  ${({ $side }) => `${$side}: -4px;`}
  width: calc(${PILLAR} + 8px);
  border: 2px solid ${({ theme }) => theme.furniture.line};
  border-radius: 2px;
  background: linear-gradient(
    180deg,
    ${({ theme }) => theme.furniture.bevel},
    ${({ theme }) => theme.furniture.frame} 45%,
    ${({ theme }) => theme.furniture.groove}
  );
  box-shadow: 0 2px 3px rgba(0, 0, 0, 0.35);
  pointer-events: none;
`;

/* 받침대 */
const Plinth = styled.div`
  height: 20px;
  margin: 0 -6px;
  border-top: 2px solid ${({ theme }) => theme.furniture.line};
  border-radius: 0 0 10px 10px;
  box-shadow: 0 0 0 1px ${({ theme }) => theme.furniture.line};
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.1), transparent 35%),
    ${({ theme }) => theme.furniture.frame};
  filter: drop-shadow(0 10px 10px rgba(17, 17, 17, 0.3));

  /* 꼬임 줄무늬 장식 */
  &::before {
    content: '';
    display: block;
    height: 8px;
    margin: 4px 14px 0;
    border-block: 1px solid ${({ theme }) => theme.furniture.groove};
    background: repeating-linear-gradient(
      135deg,
      ${({ theme }) => theme.furniture.groove} 0 2px,
      transparent 2px 6px
    );
  }
`;

/* 칸마다 원목 뒷판(세로 판자 결). 첫 칸의 윗부분은 아치 모양 */
const Shelf = styled.section`
  padding-top: ${({ theme }) => theme.space(5)};
  border: solid ${({ theme }) => theme.furniture.line};
  border-width: 0 2px;
  /* 기둥 안쪽 모서리가 앞으로 튀어나온 느낌 */
  box-shadow:
    -2px 0 0 ${({ theme }) => theme.furniture.bevel},
    2px 0 0 ${({ theme }) => theme.furniture.bevel};
  background:
    /* 기둥 옆은 움푹 들어가 어둡게 */
    linear-gradient(90deg, rgba(0, 0, 0, 0.45), transparent 14%, transparent 86%, rgba(0, 0, 0, 0.45)),
    repeating-linear-gradient(
      90deg,
      transparent 0 46px,
      rgba(0, 0, 0, 0.22) 46px 47px,
      rgba(255, 255, 255, 0.04) 47px 48px
    ),
    linear-gradient(
      180deg,
      ${({ theme }) => theme.furniture.wood},
      ${({ theme }) => theme.furniture.woodDark}
    );

  &:first-of-type {
    padding-top: ${({ theme }) => theme.space(11)};
    border-top-width: 2px;
    border-radius: 50% 50% 0 0 / 52px 52px 0 0;
    box-shadow:
      -2px 0 0 ${({ theme }) => theme.furniture.bevel},
      2px 0 0 ${({ theme }) => theme.furniture.bevel},
      inset 0 14px 18px -10px rgba(0, 0, 0, 0.55);
  }
`;

const shelfGrid = `
  display: grid;
  grid-template-columns: repeat(${BOOKS_PER_SHELF}, minmax(0, 1fr)); /* 긴 저자명이 칸을 늘리지 않도록 */
  gap: 14px;
  padding: 0 14px;
`;

const BookRow = styled.div`
  ${({ $spine }) =>
    $spine
      ? css`
          display: flex;
          align-items: flex-end;
          gap: 3px;
          padding: 0 14px;
        `
      : shelfGrid}
  align-items: ${({ $spine }) => ($spine ? 'flex-end' : 'end')};
`;

/* 책등 보기: 편집 모드에서 수정·삭제 버튼이 놓일 선반 아래 공간 */
const SpineFooter = styled.div`
  height: ${({ $editing, theme }) => ($editing ? '84px' : theme.space(4))};
  transition: height ${({ theme }) => theme.motion.fast};
`;

const SpineNew = styled.span`
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: ${({ theme }) => theme.furniture.frame};
  background: ${({ theme }) => theme.furniture.trim};
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

const SortableCell = styled.div`
  position: relative;
  flex-shrink: 0;
  z-index: ${({ $dragging }) => ($dragging ? 10 : 'auto')};
  filter: ${({ $dragging }) => ($dragging ? 'drop-shadow(0 8px 10px rgba(0, 0, 0, 0.5))' : 'none')};
`;

/* 편집 모드에서 책을 잡는 부분 */
const DragArea = styled.div`
  cursor: ${({ $on }) => ($on ? 'grab' : 'pointer')};
  outline: none;

  &:active {
    cursor: ${({ $on }) => ($on ? 'grabbing' : 'pointer')};
  }
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
  display: flex;
  gap: 2px;

  ${({ $below }) =>
    $below
      ? css`
          /* 책등은 폭이 좁아 선반 아래에 세로로 쌓는다 */
          top: calc(100% + 24px);
          left: 50%;
          flex-direction: column;
          gap: 6px;
          transform: translateX(-50%);
        `
      : css`
          top: -10px;
          right: -10px;
        `}
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

/* 책이 올라앉은 오크 선반 */
const Plank = styled.div`
  height: 16px;
  border-block: 2px solid ${({ theme }) => theme.furniture.line};
  background: linear-gradient(
    180deg,
    ${({ theme }) => theme.furniture.plank} 0 2px,
    ${({ theme }) => theme.furniture.plankEdge} 2px 4px, /* 윗선 아래로 반사광 */
    ${({ theme }) => theme.furniture.plank} 4px
  );
  box-shadow: 0 8px 12px -4px rgba(0, 0, 0, 0.55);
`;

const CaptionRow = styled.div`
  ${shelfGrid}
  margin-top: ${({ theme }) => theme.space(3)};
  padding-bottom: ${({ theme }) => theme.space(4)};
`;

const Caption = styled.div`
  min-width: 0;
`;

const CaptionTitle = styled.p`
  font-size: ${({ theme }) => theme.fontSize.sm};
  font-weight: 700;
  letter-spacing: -0.02em;
  color: ${({ theme }) => theme.furniture.label};
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
  color: ${({ theme }) => theme.furniture.labelMuted};
  font-variant-numeric: tabular-nums;
`;
